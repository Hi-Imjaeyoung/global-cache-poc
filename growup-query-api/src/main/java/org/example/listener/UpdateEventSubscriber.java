package org.example.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.CampaignRedisCacheManager;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignUpdateEvent;
import org.example.service.LazySegmentTreeService;
import org.example.service.RedisService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final Executor updateWorkerExecutor;
    private final CampaignRedisCacheManager campaignRedisCacheManager;
    private final LazySegmentTreeService lazySegmentTreeService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CampaignUpdateEvent body = objectMapper.readValue(message.getBody(), CampaignUpdateEvent.class);
            String email = body.getEmail();
            int year = body.getYear();
            log.info("[Redis Pub/Sub 수신] 업데이트 이벤트 수신: 이벤트 Id {}", body.getUpdateNumber());
            if(redisService.checkDistributedLock(email,year)){
                //업데이트 실시 ->  스레드 넘기자.
                updateWorkerExecutor.execute(() -> {
                    try {
                        // 여기서 실제 업데이트 로직 실행
                        AllCampaignTypeData[] oldCacheTree = campaignRedisCacheManager.getCachedTreeData(email, year);
                        if (oldCacheTree == null) {
                            log.info("[Update Pass] 업데이트 L2 레이어 누락으로 패스: 이벤트 Id {}", body.getUpdateNumber());
                        }else{
                            lazySegmentTreeService.updateTreeByPeriodData(oldCacheTree, body.getData());
                            campaignRedisCacheManager.saveSegTreeData(email, year, oldCacheTree);
                        }
                    } finally {
                        log.info("[Update Finish] 업데이트 성공 Lock해제: 이벤트 Id {}", body.getUpdateNumber());
                        // 반드시 락 해제!
                        redisService.releaseDistributedLock(email,year);
                    }
                });
            }else{
                log.warn("[Update Pass] Lock 습득에 실패했습니다.: 이벤트 Id{}", body.getUpdateNumber());
            }
        } catch (Exception e) {
            log.error("[Redis Pub/Sub 수신 에러] L1 무효화 처리 중 오류 발생", e);
        }
    }
}
