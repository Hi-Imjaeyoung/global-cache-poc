package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.RedisPubSubConfig;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignUpdateEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@Slf4j
public class RedisEventPublishService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private int updateCount;

    public RedisEventPublishService(StringRedisTemplate stringRedisTemplate,
                                    ObjectMapper objectMapper){
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.updateCount = 0;
    }

    public void publishUpdateEvent(String memberEmail, int year,  Map<LocalDate, AllCampaignTypeData> data) {
        try {
            updateCount ++;
            CampaignUpdateEvent campaignUpdateEvent = CampaignUpdateEvent.builder()
                    .email(memberEmail)
                    .data(data)
                    .year(year)
                    .updateNumber(updateCount)
                    .build();
            String message = objectMapper.writeValueAsString(campaignUpdateEvent);
            String topic = RedisPubSubConfig.UPDATE_CACHE_DATA;
            stringRedisTemplate.convertAndSend(topic, message);
            log.info("업데이트 이벤트 발행 :{}",memberEmail);
        }catch (JsonProcessingException e){
            log.error("Json Paring 중 에러발생 :{}",e.getMessage());
        }

    }
}
