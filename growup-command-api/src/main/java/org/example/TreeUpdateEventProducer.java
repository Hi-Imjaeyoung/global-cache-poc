package org.example;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreeUpdateEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "tree-update-topic";

    public void sendUpdateEvent(Long campaignId) {
        String message = "UPDATE_CAMPAIGN_" + campaignId;
        kafkaTemplate.send(TOPIC, String.valueOf(campaignId), message);
        log.info("🚀 [Kafka 이벤트 발행 완료] 토픽: {}, 메시지: {}", TOPIC, message);
    }
}
