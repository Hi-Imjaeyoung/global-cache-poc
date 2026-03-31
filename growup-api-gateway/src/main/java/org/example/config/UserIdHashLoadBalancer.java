package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class UserIdHashLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    public UserIdHashLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable();
        if (supplier == null) {
            log.warn("🚨 [CustomLB] Supplier가 NULL 입니다!");
            return Mono.just(new EmptyResponse());
        }

        return supplier.get(request).next()
                .flatMap(instances -> getInstanceResponseAsync(instances, request));
    }

    private Mono<Response<ServiceInstance>> getInstanceResponseAsync(List<ServiceInstance> instances, Request request) {
        if (instances.isEmpty()) {
            log.warn("[CustomLB] 살아있는 {} 서버가 0대입니다!", serviceId);
            return Mono.just(new EmptyResponse());
        }

        String userId = null;
        if (request.getContext() instanceof RequestDataContext) {
            RequestDataContext context = (RequestDataContext) request.getContext();
            userId = context.getClientRequest().getHeaders().getFirst("X-User-Id");
        }

        if (userId == null || userId.isBlank()) {
            int randomIndex = ThreadLocalRandom.current().nextInt(instances.size());
            log.info("[CustomLB] User-Id 없음! 랜덤 {}번 인스턴스로 쏜다!", randomIndex);
            return Mono.just(new DefaultResponse(instances.get(randomIndex)));
        }

        List<ServiceInstance> sortedInstances = instances.stream()
                .sorted(Comparator.comparing(ServiceInstance::getInstanceId))
                .toList();

        int hash = Math.abs(userId.hashCode());
        int targetIndex = hash % instances.size();

        ServiceInstance chosenInstance = sortedInstances.get(targetIndex);

        // 여기서 타겟 서버가 결정됨! (info로 격상!)
        log.info("🎯 [CustomLB] User {} 의 1차 타겟: {} 서버 (포트: {})",
                userId, chosenInstance.getInstanceId(), chosenInstance.getPort());

        //  Ping 쏘기 시작
        return isAliveAsync(chosenInstance)
                .map(isAlive -> {
                    if (isAlive) {
                        log.info("[CustomLB] 핑 성공! {} 서버로 정상 라우팅!", chosenInstance.getPort());
                        return new DefaultResponse(chosenInstance);
                    } else {
                        //핑 실패 우회 시!
                        if (instances.size() == 1) {
                            log.error("[CustomLB] 유일한 서버가 죽었습니다! 우회할 곳이 없습니다!");
                            return new DefaultResponse(chosenInstance);
                        }

                        int fallbackIndex = (targetIndex + 1) % instances.size();
                        ServiceInstance fallbackInstance = sortedInstances.get(fallbackIndex);

                        log.warn("🚨 [CustomLB] {}번 서버 유령 감지!! ➡️ {}번 서버로 긴급 우회!!",
                                chosenInstance.getPort(), fallbackInstance.getPort());

                        return new DefaultResponse(fallbackInstance);
                    }
                });
    }

    private Mono<Boolean> isAliveAsync(ServiceInstance instance) {
        return Mono.fromCallable(() -> {
            try (Socket socket = new Socket()) {
                InetSocketAddress address = InetSocketAddress.createUnresolved(instance.getHost(), instance.getPort());
                socket.connect(new InetSocketAddress(address.getHostName(), address.getPort()), 50);
                return true;
            } catch (Exception e) {
                log.warn("[Ping 실패] {} 찌르기 실패 (원인: {})", instance.getPort(), e.getMessage());
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}