package org.example;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class TreeUpdateEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "tree-update-topic";
    private final ObjectMapper objectMapper;

    public void sendUpdateEvent(String email,int year,Map<LocalDate, AllCampaignTypeData> deletedData) {
        try {
            TreeUpdateEvent event = new TreeUpdateEvent(email,year,deletedData);
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC,email,message);
            log.debug("[Kafka 이벤트 발행 완료] 토픽: {}, 메시지: {}", TOPIC, message);
        }catch (JsonProcessingException e){
            log.error("update 이벤트 발행 중 Json 변환 에러 발생 에러 메시지 :{}",e.getMessage());
        }
    }
}
