package org.example.facade;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignAnalysisDto;
import org.example.dto.TotalCampaignsData;
import org.example.listener.TreeBuildEvent;
import org.example.service.LegacyKeywordService;
import org.example.service.LegacyLazySegmentTreeService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class LegacyCampaignTotalDataFacade {

    private final LegacyKeywordService keywordService;
    private final LegacyLazySegmentTreeService lazySegmentTreeService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public TotalCampaignsData getCampaignTotalDataByLazyLoadingTree(String email, LocalDate start, LocalDate end){
//        Map<String, CampaignAnalysisDto> campaignAnalysisDataKeyCampaignName =
//                keywordService.getEachCampaignAdCostSumAndAdSalesByPeriodAndEmail(email,start,end);
        AllCampaignTypeData allCampaignTypeData;
        if(lazySegmentTreeService.isTreeBuild(email,start.getYear())){
            allCampaignTypeData =
                    lazySegmentTreeService.getCachedOrSelectAllCampaignTypeDataByPeriod(email,start,end);
            return TotalCampaignsData.builder()
                    .adSalesAndAdCostByCampaignName(new HashMap<>())
                    .sumOfAdSalesAndAdCostByCampaignType(new HashMap<>(allCampaignTypeData.getCampaignAnalysisDtoMap()))
                    .build();
        }
        eventPublisher.publishEvent(new TreeBuildEvent(email,start.getYear()));
        if(end.getYear() != start.getYear()) eventPublisher.publishEvent(new TreeBuildEvent(email,start.getYear()));
        Map<String, CampaignAnalysisDto> campaignAnalysisDataKeyCampaignType =
                keywordService.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(email,start,end);
        return TotalCampaignsData.builder()
                .adSalesAndAdCostByCampaignName(new HashMap<>())
                .sumOfAdSalesAndAdCostByCampaignType(campaignAnalysisDataKeyCampaignType)
                .build();
    }

}
