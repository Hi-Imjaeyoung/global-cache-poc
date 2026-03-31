package org.example.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.TreeUpdateEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreeUpdateEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "tree-update";
    private final ObjectMapper objectMapper;

    public void sendUpdateEvent(String email,Long memberId,int year,Map<LocalDate, AllCampaignTypeData> deletedData) {
        try {
            String partitionKey = String.valueOf(memberId);
            String eventId = UUID.randomUUID().toString();
            TreeUpdateEvent event = new TreeUpdateEvent(email,year,deletedData,eventId);
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC,email,message)
                            .whenComplete((result,ex)->{
                                if(ex == null){
                                    log.info("[Kafka] 이벤트 발행 성공! [Topic: {}, Partition: {}, Key: {}]",
                                            result.getRecordMetadata().topic(),
                                            result.getRecordMetadata().partition(), // 할당된 파티션 번호 확인!
                                            partitionKey);
                                }else{
                                    log.error("[Kafka] 이벤트 발행 중 오류 발생 Key:{} 에러메시지 :{}",
                                            partitionKey,
                                            ex.getMessage());
                                }
                            });
        }catch (JsonProcessingException e){
            log.error("update 이벤트 발행 중 Json 변환 에러 발생 에러 메시지 :{}",e.getMessage());
        }
    }
}
