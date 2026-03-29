    package org.example.config;

    import org.springframework.beans.factory.ObjectProvider;
    import org.springframework.cloud.client.ServiceInstance;
    import org.springframework.cloud.client.loadbalancer.*;
    import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
    import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
    import org.springframework.http.HttpHeaders;
    import reactor.core.publisher.Mono;

    import java.util.Comparator;
    import java.util.List;
    import java.util.concurrent.ThreadLocalRandom;

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
                return Mono.just(new EmptyResponse());
            }
            // 살아있는 서버(Instance) 리스트를 가져와서 해싱 로직으로 넘김!
            return supplier.get(request).next()
                    .map(instances -> getInstanceResponse(instances, request));
        }

        private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, Request request) {
            if (instances.isEmpty()) {
                System.out.println("🚨 [LoadBalancer] 살아있는 " + serviceId + " 서버가 없습니다!");
                return new EmptyResponse();
            }
            String userId = null;
            // 1. request의 Context가 RequestDataContext 타입인지 확인
            if (request.getContext() instanceof RequestDataContext) {
                RequestDataContext context = (RequestDataContext) request.getContext();

                // 2. 보따리(Context) 안에서 진짜 ClientRequest를 꺼내고, 거기서 헤더를 조회!
                // HTTP 헤더는 대소문자를 구분하지 않지만, getFirst()로 안전하게 꺼내기
                userId = context.getClientRequest().getHeaders().getFirst("X-User-Id");
            }

            // 만약 userId가 없으면? 그냥 아무 데나 랜덤으로 던지기 (예외 처리)
            if (userId == null || userId.isBlank()) {
                int randomIndex = ThreadLocalRandom.current().nextInt(instances.size());
                return new DefaultResponse(instances.get(randomIndex));
            }

            // Eureka 에서 일정한 순서로 정렬하여, 서버 순서 보장.
            List<ServiceInstance> sortedInstances = instances.stream()
                    .sorted(Comparator.comparing(ServiceInstance::getInstanceId))
                    .toList();

            int hash = Math.abs(userId.hashCode());
            int targetIndex = hash % instances.size();

            ServiceInstance chosenInstance = instances.get(targetIndex);
            System.out.println("🎯 [Hash Routing] User " + userId + " ➡️ " + chosenInstance.getInstanceId() + " 서버로 직행!");

            return new DefaultResponse(chosenInstance);
        }
    }