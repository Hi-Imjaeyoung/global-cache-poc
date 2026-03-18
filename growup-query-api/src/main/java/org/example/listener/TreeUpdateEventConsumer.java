package org.example.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.TreeUpdateEvent;
import org.example.service.LazySegmentTreeService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TreeUpdateEventConsumer {

    private final ObjectMapper objectMapper;
    private final LazySegmentTreeService lazySegmentTreeService;
    private final ApplicationEventPublisher applicationEventPublisher;
    @KafkaListener(
            topics = "tree-update-topic",
            groupId = "query-api-group-${random.uuid}"
    )
    public void listenTreeUpdate(String message) {
        try {
            TreeUpdateEvent event = objectMapper.readValue(message, TreeUpdateEvent.class);
            String memberEmail = event.memberEmail();
            Map<LocalDate, AllCampaignTypeData> deltaData = event.deltaData();
            int year = event.year();
            log.debug("[Kafka 수신 완료] 회원: {}, 수신된 변화량 크기: {}일치", memberEmail, deltaData.size());
            if(lazySegmentTreeService.isTreeBuild(memberEmail,year)){
//                applicationEventPublisher.publishEvent(event);
                // 불필요한 비동기 작업 제거.
                lazySegmentTreeService.updateTreeByPeriodData(memberEmail,deltaData);
                log.debug("[트리 업데이트 완료] {} 님의 메모리 캐시가 최신화되었습니다.", memberEmail);
                return;
            }
            log.debug("[트리 업데이트 미실시] {} 님의 {} 년 트리가 존재하지 않습니다",memberEmail,year);
        } catch (JsonProcessingException e) {
            log.error("[Kafka 수신 실패] JSON 파싱 에러! 메시지: {}, 원인: {}", message, e.getMessage());
        } catch (Exception e) {
            log.error("[트리 업데이트 실패] 알 수 없는 에러 발생!", e);
        }
    }
}
