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
    public void listenTreeUpdate(String message) throws JsonProcessingException {
        String memberEmail = null;
        try {
            TreeUpdateEvent event = objectMapper.readValue(message, TreeUpdateEvent.class);
            memberEmail = event.memberEmail();
            Map<LocalDate, AllCampaignTypeData> updateData = event.deltaData();
            int year = event.year();
            log.debug("[Kafka 수신 완료] 회원: {}, 수신된 변화량 크기: {}일치", memberEmail, updateData.size());
            // L1 remove Event
            String invalidationMsg = memberEmail + ":" + year;
            String topic = RedisPubSubConfig.L1_CACHE_EVICT_TOPIC; // "l1-cache-evict-topic"
            stringRedisTemplate.convertAndSend(topic, invalidationMsg);
            log.debug("[1차 L1 삭제 방송 송출] {}", invalidationMsg);
            // L2 data update & save
            AllCampaignTypeData[] oldCacheTree = campaignRedisCacheManager.getCachedTreeData(memberEmail, year);
            if (oldCacheTree == null) {
                log.debug("[Redis] L2 캐시가 존재하지 않습니다.");
                return;
            }
            lazySegmentTreeService.updateTreeByPeriodData(oldCacheTree,updateData);
            campaignRedisCacheManager.saveRawData(memberEmail,year,oldCacheTree);
            // L1 remove Event
            CompletableFuture.runAsync(()->{
                stringRedisTemplate.convertAndSend(topic, invalidationMsg);
                log.debug("[2차 L1 삭제 방송 송출] {}", invalidationMsg);
            },CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS));
        } catch (JsonProcessingException e) {
            log.error("[Kafka 수신 실패] JSON 파싱 에러! 발생 회원: {}, 원인: {}", memberEmail, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[Kafka 수신 실패] 알수없는 오류! 발생 회원: {}, 원인:{}", memberEmail, e.getMessage());
            throw new GrouException(ErrorCode.UNKNOWN_ERROR);
        }
    }
}
