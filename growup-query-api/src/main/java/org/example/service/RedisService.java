package org.example.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.RedisPubSubConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@AllArgsConstructor
public class RedisService {
    private final StringRedisTemplate stringRedisTemplate;

    @CircuitBreaker(name="redisCircuitBreaker",fallbackMethod = "fallbackPublishInvalidationL1CacheTree")
    public boolean publishInvalidationL1CacheTree(String memberEmail, int year){
        String invalidationMsg = memberEmail + ":" + year;
        String topic = RedisPubSubConfig.L1_CACHE_EVICT_TOPIC;
        stringRedisTemplate.convertAndSend(topic, invalidationMsg);
        return true;
    }

    public boolean fallbackPublishInvalidationL1CacheTree(String memberEmail, int year,Throwable t){
        log.warn("[서킷 OPEN] Redis 장애로 PubSub 기능 불가. 이벤트 업데이트 종료");
        return false;
    }

    public void scheduleDelayedPublishInvalidationL1CacheTree(String memberEmail, int year) {
        String invalidationMsg = memberEmail + ":" + year;
        String topic = RedisPubSubConfig.L1_CACHE_EVICT_TOPIC;
        CompletableFuture.runAsync(() -> {
            stringRedisTemplate.convertAndSend(topic, invalidationMsg);
            log.debug("[2차 L1 삭제 방송 송출] {}", invalidationMsg);
        }, CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS));
    }

    @CircuitBreaker(name="redisCircuitBreaker",fallbackMethod = "fallbackCheckIdempotency")
    public boolean checkIdempotency(String idempotencyKey){
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "DONE", 24, TimeUnit.HOURS);

        if (result == null) throw new RuntimeException("Redis 응답 없음");
        return result;
    }
    public boolean fallbackCheckIdempotency(String key, Throwable t) {
        log.warn("[서킷 OPEN] Redis 장애로 멱등성 체크 불가. 안전을 위해 업데이트를 스킵합니다.");
        return false;
    }

    @CircuitBreaker(name="redisCircuitBreaker",fallbackMethod = "fallbackDeleteIdempotency")
    public void deleteIdempotency(String idempotencyKey){
        Boolean result = stringRedisTemplate.delete(idempotencyKey); // 삭제로 변경!
        if (Boolean.FALSE.equals(result)) log.warn("삭제할 멱등성 키가 없습니다.");
    }
    public void fallbackDeleteIdempotency(String idempotencyKey,Throwable t){
        log.warn("[서킷 OPEN] Redis 장애로 멱등성 키 삭제 불가 키: {}",idempotencyKey);
    }


}
