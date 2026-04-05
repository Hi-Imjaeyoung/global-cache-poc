package org.example.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.TreeUpdateEvent;
import org.example.config.CampaignRedisCacheManager;
import org.example.service.LazySegmentTreeService;
import org.example.service.RedisService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TreeUpdateEventConsumer {

    private final ObjectMapper objectMapper;
    private final LazySegmentTreeService lazySegmentTreeService;
    private final CampaignRedisCacheManager campaignRedisCacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisService redisService;

    @KafkaListener(
            topics = "tree-update",
            groupId = "query-consumer-group"
    )
    public void listenTreeUpdate(String message) throws Exception {
        TreeUpdateEvent event = objectMapper.readValue(message, TreeUpdateEvent.class);
        String memberEmail = event.memberEmail();
        String idempotencyKey = "event:processed:" + event.eventId();
        if (!redisService.checkIdempotency(idempotencyKey)) {
            log.info("[이벤트 드롭] 중복 수신 또는 Redis 장애로 인해 처리를 중단합니다. (EventID: {})", event.eventId());
            return;
        }
        log.debug("[Kafka 수신 완료] 회원: {}, 수신된 변화량 크기: {}일치", memberEmail, event.deltaData().size());
        // 메인 비즈니스 로직
        try {
            // [1차 L1 삭제 방송]
            if(!redisService.publishInvalidationL1CacheTree(memberEmail,event.year())){
                log.info("[이벤트 드롭] Redis 장애로 인해 처리를 중단합니다. (EventID: {})", event.eventId());
                return;
            }
            // [L2 업데이트]
            AllCampaignTypeData[] oldCacheTree = campaignRedisCacheManager.getCachedTreeData(memberEmail, event.year());
            if (oldCacheTree != null) {
                lazySegmentTreeService.updateTreeByPeriodData(oldCacheTree, event.deltaData());
                campaignRedisCacheManager.saveSegTreeData(memberEmail, event.year(), oldCacheTree);
            }
            // [2차 비동기 L1 삭제 방송]
            redisService.scheduleDelayedPublishInvalidationL1CacheTree(memberEmail, event.year());
        } catch (Exception e) {
            // [보상 트랜잭션] 실패 시 멱등성 키 삭제
            log.warn("로직 처리 실패! 카프카 재시도를 위해 멱등성 방패를 해제합니다. 원인: {}", e.getMessage());
            redisService.deleteIdempotency(idempotencyKey);
            throw e;
        }
    }
}
