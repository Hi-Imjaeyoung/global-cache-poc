package org.example.circuitBreaker;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.example.QueryServerApplication;
import org.example.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = QueryServerApplication.class)
@Testcontainers
public class RedisCircuitBreakerChaosTest {

    // 1. Docker 네트워크 생성 (Toxiproxy와 Redis를 같은 망에 묶기 위함)
    private static final Network network = Network.newNetwork();

    // 2. 실제 Redis 컨테이너 (이름을 "redis"로 지정해서 네트워크 내부에서 찾을 수 있게 함)
    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0-alpine")
            .withExposedPorts(6379)
            .withNetwork(network)
            .withNetworkAliases("redis");

    // 3. Toxiproxy 컨테이너 (Redis 앞단에 서는 프록시)
    @Container
    private static final ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
            .withNetwork(network);

    private static Proxy redisProxy; // ContainerProxy 대신 eu.rekawek.toxiproxy.Proxy 사용

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // 1. Toxiproxy 호스트와 포트로 클라이언트 생성
        ToxiproxyClient client = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());

        try {
            // 2. 프록시 생성 (이미 있으면 가져오고 없으면 만듦)
            // "redis-proxy"라는 이름으로 앱 -> Toxiproxy -> redis:6379 연결
            redisProxy = client.createProxy("redis-proxy", "0.0.0.0:8666", "redis:6379");

            // 3. 앱이 바라볼 포트는 Toxiproxy가 외부로 열어준 매핑 포트여야 함!
            int proxyPort = toxiproxy.getMappedPort(8666);

            registry.add("spring.data.redis.host", toxiproxy::getHost);
            registry.add("spring.data.redis.port", () -> proxyPort);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Autowired
    private RedisService redisService; // 테스트할 비즈니스 로직 (서킷 브레이커 적용됨)

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        // 서킷 브레이커 인스턴스 가져오기 (이름은 application.yml에 설정한 이름)
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("redisCircuitBreaker");

        // 테스트 전 서킷을 초기화(CLOSED) 상태로 되돌림
        circuitBreaker.transitionToClosedState();
    }

    @Test
    void testCircuitBreakerOpensWhenRedisIsTooSlow() throws Exception {
        // [Given] 정상 상태 검증
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // ☠️ [Chaos Injection] Toxiproxy에 독(Toxic) 타기!
        // 다운스트림(Redis로 가는 방향)에 500ms 지연을 주입한다. (Jitter는 변동성 0ms)
        redisProxy.toxics().latency("redis-latency", ToxicDirection.DOWNSTREAM, 500);

        // [When] 트래픽 쏟아붓기
        // application.yml에 minimumNumberOfCalls가 10이므로, 최소 10번은 찔러봐야 서킷이 계산을 시작함
        for (int i = 0; i < 10; i++) {
            try {
                // 이 호출은 500ms 지연 때문에 TimeLimiter(예: 200ms)에 걸려 Timeout 예외가 발생해야 함!
                redisService.checkIdempotency(UUID.randomUUID().toString());
            } catch (Exception e) {
                // 지연으로 인한 예외 발생 (정상적인 실패 흐름)
                System.out.println("요청 실패: " + e.getMessage());
            }
        }

        // [Then] 서킷 브레이커 상태 검증
        // 10번 중 15% 이상 실패(Timeout)했으므로 상태가 OPEN으로 바뀌었는지 확인!
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        System.out.println("✅ 성공: 네트워크 지연으로 인해 서킷 브레이커가 정상적으로 OPEN 되었습니다!");
    }
}