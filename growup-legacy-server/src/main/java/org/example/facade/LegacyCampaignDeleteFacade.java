package org.example.facade;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignDeleteDto;
import org.example.listener.TreeBuildEvent;
import org.example.listener.TreeUpdateEvent;
import org.example.service.LegacyKeywordService;
import org.example.service.LegacyLazySegmentTreeService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class LegacyCampaignDeleteFacade {

    private final LegacyKeywordService keywordService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Map<String,Integer> deleteCampaignDataByPeriod(CampaignDeleteDto campaignDeleteDto){
        //임계값 확인
        boolean checkThreshold = campaignDeleteDto.checkThreshold();
        Map<LocalDate, AllCampaignTypeData> extractDeleteData = null;
        String email = campaignDeleteDto.getEmail();
        if(checkThreshold){
        extractDeleteData =
            keywordService.extractDeleteCampaignDataByPeriod(campaignDeleteDto.getStart(),campaignDeleteDto.getEnd(),campaignDeleteDto.getCampaignIds());
        }
        Map<String,Integer> result = new HashMap<>();
        result.put("keyword",keywordService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
        if(checkThreshold && extractDeleteData != null){
            applicationEventPublisher.publishEvent(new TreeUpdateEvent(email,extractDeleteData));
        }else{
            int year = campaignDeleteDto.getStart().getYear();
            applicationEventPublisher.publishEvent(new TreeBuildEvent(email,year));
        }
        return result;
    }
}
