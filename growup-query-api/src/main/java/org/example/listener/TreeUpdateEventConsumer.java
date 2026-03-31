package org.example.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.RedisPubSubConfig;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.TreeUpdateEvent;
import org.example.exception.ErrorCode;
import org.example.exception.GrouException;
import org.example.config.CampaignRedisCacheManager;
import org.example.service.LazySegmentTreeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TreeUpdateEventConsumer {

    private final ObjectMapper objectMapper;
    private final LazySegmentTreeService lazySegmentTreeService;
    private final CampaignRedisCacheManager campaignRedisCacheManager;
    private final StringRedisTemplate stringRedisTemplate;

    @KafkaListener(
            topics = "tree-update",
            groupId = "query-consumer-group"
    )
    @KafkaListener(topics = "tree-update", groupId = "query-consumer-group")
    public void listenTreeUpdate(String message) throws Exception {
        TreeUpdateEvent event = objectMapper.readValue(message, TreeUpdateEvent.class);
        String memberEmail = event.memberEmail();
        String idempotencyKey = "event:processed:" + event.eventId();

        Boolean isFirstTime = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "DONE", 24, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.warn("[멱등성 방어] 이미 처리된 이벤트입니다!! (EventID: {})", event.eventId());
            return;
        }

        log.debug("[Kafka 수신 완료] 회원: {}, 수신된 변화량 크기: {}일치", memberEmail, event.deltaData().size());

        // 메인 비즈니스 로직 (하나의 얇은 try-catch로 묶기!)
        try {
            // [1차 L1 삭제 방송]
            String invalidationMsg = memberEmail + ":" + event.year();
            String topic = RedisPubSubConfig.L1_CACHE_EVICT_TOPIC;
            stringRedisTemplate.convertAndSend(topic, invalidationMsg);

            // [L2 업데이트]
            AllCampaignTypeData[] oldCacheTree = campaignRedisCacheManager.getCachedTreeData(memberEmail, event.year());
            if (oldCacheTree != null) {
                lazySegmentTreeService.updateTreeByPeriodData(oldCacheTree, event.deltaData());
                campaignRedisCacheManager.saveRawData(memberEmail, event.year(), oldCacheTree);
            }

            scheduleDelayedEviction(topic, invalidationMsg);
        } catch (Exception e) {
            // [보상 트랜잭션] 실패 시 멱등성 키 삭제
            log.warn("로직 처리 실패! 카프카 재시도를 위해 멱등성 방패를 해제합니다. 원인: {}", e.getMessage());
            stringRedisTemplate.delete(idempotencyKey);
            throw e;
        }
    }

    private void scheduleDelayedEviction(String topic, String msg) {
        CompletableFuture.runAsync(() -> {
            stringRedisTemplate.convertAndSend(topic, msg);
            log.debug("[2차 L1 삭제 방송 송출] {}", msg);
        }, CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS));
    }
}
