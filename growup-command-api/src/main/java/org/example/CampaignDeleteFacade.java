package org.example;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignDeleteDto;
import org.example.service.KeywordCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class CampaignDeleteFacade {
    private final CampaignService campaignService;
    private final KeywordCommandService keywordCommandService;
    private final TreeUpdateEventProducer treeUpdateEventProducer;

    @Transactional
    public Map<String,Integer> deleteCampaignDataByPeriod(CampaignDeleteDto campaignDeleteDto){
        //임계값 확인
        boolean checkThreshold = campaignDeleteDto.checkThreshold();
        String email  = campaignDeleteDto.getEmail();
        Map<LocalDate, AllCampaignTypeData> extractDeleteData = null;
        if(checkThreshold){
            //삭제 전 데이터 조회 Master DB...할게?
        extractDeleteData =
                keywordCommandService.extractDeleteCampaignDataByPeriod(campaignDeleteDto.getStart(),campaignDeleteDto.getEnd(),campaignDeleteDto.getCampaignIds());
        }
//        Map<String,Integer> result = new HashMap<>();
//        result.put("keyword",keywordService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
//        result.put("margin",marginService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
//        List<Long> executionIds = new ArrayList<>();
//        for(Long campaignId : campaignDeleteDto.getCampaignIds()){
//            executionIds.addAll(executionService.getMyExecutionData(campaignId).stream().map(ExecutionMarginResDto::getExeId).toList());
//        }
//        result.put("campaignOptionDetail",campaignOptionDetailsService.deleteKeywordByExecutionIdsAndDate(executionIds,campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
//        result.put("memo",memoService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
        if(checkThreshold && extractDeleteData != null){
            int year = campaignDeleteDto.getStart().getYear();
            treeUpdateEventProducer.sendUpdateEvent(email,year,extractDeleteData);
        }else{
            int year = campaignDeleteDto.getStart().getYear();
            // 임계값 초과 요청으로 인한 삭제 요청 이벤트 발생
//            treeUpdateEventProducer.sendUpdateEvent();
        }
        return new HashMap<>();
    }
}
