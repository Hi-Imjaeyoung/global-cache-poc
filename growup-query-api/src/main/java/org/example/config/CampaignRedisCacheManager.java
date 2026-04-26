package org.example.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignRedisCacheManager {

    private final StringRedisTemplate redisTemplate; // 가장 가볍고 빠른 String 전용 템플릿
    private final ObjectMapper objectMapper;

    @TimeLimiter(name = "redisTL", fallbackMethod = "fallbackGetCachedTreeData")
    @CircuitBreaker(name="redisCircuitBreaker",fallbackMethod ="fallbackGetCachedTreeData" )
    @Bulkhead(name = "redisBulkhead", fallbackMethod = "fallbackGetCachedTreeData", type = Bulkhead.Type.SEMAPHORE)
    public AllCampaignTypeData[] getCachedTreeData(String email, int year){
        String key = "raw-data:"+email+":"+year;
        String json = redisTemplate.opsForValue().get(key);

        if(json == null) return null;

        try{
            return objectMapper.readValue(json, AllCampaignTypeData[].class);
        } catch (JsonMappingException e) {
            log.error("[Redis] Redis 데이터 로딩 중 JsonParing 에러가 발생 : {}",e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("[Redis] Redis 데이터 로딩 중 JsonMapping 에러가 발생 : {}",e.getMessage());
        }
        return null;
    }
    public AllCampaignTypeData[] fallbackGetCachedTreeData(String email, int year,Throwable t) {
        log.warn("Redis 장애로 Null을 리턴합니다.");
        return null;
    }

    @CircuitBreaker(name="redisCircuitBreaker")
    public void saveSegTreeData(String email, int year, AllCampaignTypeData[] rawData){
        String key = "raw-data:"+email+":"+year;
        try {
            String json = objectMapper.writeValueAsString(rawData);
            redisTemplate.opsForValue().set(key, json, 24, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("Redis 캐시 저장 실패!", e);
        }
    }

    @CircuitBreaker(name="redisCircuitBreaker")
    public void removeCacheData(String email, int year){
        String key = "raw-data:"+email+":"+year;
        log.info("Redis 저장 데이터 삭제 키:{}",key);
        redisTemplate.delete(key);
    }

}
