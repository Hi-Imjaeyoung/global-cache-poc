package org.example.facade;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.CampaignRedisCacheManager;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignAnalysisDto;
import org.example.dto.TotalCampaignsData;
import org.example.listener.PrefixBuildEvent;
import org.example.listener.TreeBuildEvent;
import org.example.service.KeywordService;
import org.example.service.LazySegmentTreeService;
import org.example.service.PrefixSumService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class CampaignTotalDataFacade {

    private final KeywordService keywordService;
    private final LazySegmentTreeService lazySegmentTreeService;
    private final ApplicationEventPublisher eventPublisher;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final CampaignRedisCacheManager campaignRedisCacheManager;
    private final PrefixSumService prefixSumService;

    @Transactional(readOnly = true)
    public TotalCampaignsData getCampaignTotalDataByLazyLoadingTree(String email, LocalDate start, LocalDate end){
        if(circuitBreakerRegistry.circuitBreaker("redisCircuitBreaker").getState() == CircuitBreaker.State.OPEN){
            log.warn("Redis 서킷 Open 상태. L1 빌드 정지 상태입니다. DB 원문 조회 및 L1 무효화를 실시합니다.");
            lazySegmentTreeService.removeAllTreeData();
            Map<String, CampaignAnalysisDto> campaignAnalysisDataKeyCampaignType =
                    keywordService.getAllTypeOfCampaignAdCostAndSaleSumByCampaignType(start,end,email);
            return TotalCampaignsData.builder()
                    .adSalesAndAdCostByCampaignName(new HashMap<>())
                    .sumOfAdSalesAndAdCostByCampaignType(campaignAnalysisDataKeyCampaignType)
                    .build();
        }
        AllCampaignTypeData allCampaignTypeData;
        AllCampaignTypeData[] cachedTree = campaignRedisCacheManager.getCachedTreeData(email,start.getYear());
        lazySegmentTreeService.saveBackupTreeData(email,start.getYear(),cachedTree);
        if(lazySegmentTreeService.isTreeBuild(email,start.getYear())){
            log.debug("캐싱 트리 데이터로 조회합니다. 유저: {}",email);
            allCampaignTypeData =
                    lazySegmentTreeService.getCachedOrSelectAllCampaignTypeDataByPeriod(email,start,end);
            return TotalCampaignsData.builder()
                    .adSalesAndAdCostByCampaignName(new HashMap<>())
                    .sumOfAdSalesAndAdCostByCampaignType(new HashMap<>(allCampaignTypeData.getCampaignAnalysisDtoMap()))
                    .maxAdCost(allCampaignTypeData.getMaxAdCost())
                    .maxAdSales(allCampaignTypeData.getMaxAdSales())
                    .minAdCost(allCampaignTypeData.getMinAdCost())
                    .minAdSales(allCampaignTypeData.getMinAdSales())
                    .build();
        }
        CircuitBreaker.State currentState = circuitBreakerRegistry.circuitBreaker("redisCircuitBreaker").getState();
        if (currentState == CircuitBreaker.State.CLOSED) {
            log.debug("캐시 미스! 비동기 트리 빌드 이벤트를 발행합니다. 유저: {}", email);
            eventPublisher.publishEvent(new TreeBuildEvent(email, start.getYear()));
            if(end.getYear() != start.getYear()) {
                eventPublisher.publishEvent(new TreeBuildEvent(email, end.getYear()));
            }
        } else {
            log.warn("서킷 상태가 불완전합니다(State: {}). 빌드 이벤트를 생략하고 DB 원문으로 응답합니다.", currentState);
        }
        Map<String, CampaignAnalysisDto> campaignAnalysisDataKeyCampaignType =
                keywordService.getAllTypeOfCampaignAdCostAndSaleSumByCampaignType(start,end,email);
        return TotalCampaignsData.builder()
                .adSalesAndAdCostByCampaignName(new HashMap<>())
                .sumOfAdSalesAndAdCostByCampaignType(campaignAnalysisDataKeyCampaignType)
                .build();
    }
    @Transactional(readOnly = true)
    public TotalCampaignsData getCampaignTotalDataByPrefix(String email,LocalDate start,  LocalDate end){
        AllCampaignTypeData allCampaignTypeData;
        AllCampaignTypeData[] prefixData = campaignRedisCacheManager.getCachedPrefixData(email,start.getYear());
        prefixSumService.saveBackupPrefixData(email,start.getYear(),prefixData);
        if(prefixSumService.hasPrefixData(email,start.getYear())){
            log.debug("누적합으로 조회합니다. 유저: {}",email);
            allCampaignTypeData =
                    prefixSumService.getCachedOrSelectAllCampaignTypeDataByPeriod(email,start,end);
            return TotalCampaignsData.builder()
                    .adSalesAndAdCostByCampaignName(new HashMap<>())
                    .sumOfAdSalesAndAdCostByCampaignType(new HashMap<>(allCampaignTypeData.getCampaignAnalysisDtoMap()))
                    .maxAdCost(allCampaignTypeData.getMaxAdCost())
                    .maxAdSales(allCampaignTypeData.getMaxAdSales())
                    .minAdCost(allCampaignTypeData.getMinAdCost())
                    .minAdSales(allCampaignTypeData.getMinAdSales())
                    .build();
        }
        log.debug("캐시 미스! 비동기 누적합 설정 이벤트를 발행합니다. 유저: {}", email);
        eventPublisher.publishEvent(new PrefixBuildEvent(email, start.getYear()));
        if(end.getYear() != start.getYear()) {
            eventPublisher.publishEvent(new PrefixBuildEvent(email, end.getYear()));
        }
        Map<String, CampaignAnalysisDto> campaignAnalysisDataKeyCampaignType =
                keywordService.getAllTypeOfCampaignAdCostAndSaleSumByCampaignType(start,end,email);
        return TotalCampaignsData.builder()
                .adSalesAndAdCostByCampaignName(new HashMap<>())
                .sumOfAdSalesAndAdCostByCampaignType(campaignAnalysisDataKeyCampaignType)
                .build();
    }
}
