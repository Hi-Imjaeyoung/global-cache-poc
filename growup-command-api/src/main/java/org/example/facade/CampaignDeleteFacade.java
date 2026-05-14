package org.example.facade;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.example.exception.ErrorCode;
import org.example.exception.GrouException;
import org.example.producer.TreeUpdateEventProducer;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignDeleteDto;
import org.example.repo.MemberRepository;
import org.example.service.KeywordCommandService;
import org.example.service.RedisEventPublishService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class CampaignDeleteFacade {
    private final KeywordCommandService keywordCommandService;
    private final TreeUpdateEventProducer treeUpdateEventProducer;
    private final RedisEventPublishService redisEventPublishService;
    private final MemberRepository memberRepository;

    @Transactional
    public Map<String,Integer> deleteCampaignDataByPeriod(CampaignDeleteDto campaignDeleteDto){
        //임계값 확인
        boolean checkThreshold = campaignDeleteDto.checkThreshold();
        Long memberId = memberRepository.findByEmail(campaignDeleteDto.getEmail())
                .orElseThrow(()-> new GrouException(ErrorCode.UNKNOWN_ERROR)).getId();
        String email  = campaignDeleteDto.getEmail();
        Map<LocalDate, AllCampaignTypeData> extractDeleteData = null;
        if(checkThreshold){
            //삭제 전 데이터 조회 Master DB...할게?
        extractDeleteData =
                keywordCommandService.extractDeleteCampaignDataByPeriod(campaignDeleteDto.getStart(),campaignDeleteDto.getEnd(),campaignDeleteDto.getCampaignIds());
        }
        keywordCommandService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd());
        if(checkThreshold && extractDeleteData != null){
            int year = campaignDeleteDto.getStart().getYear();
            // Publish By Kafka
            treeUpdateEventProducer.sendUpdateEvent(email,memberId,year,extractDeleteData);
        }else{
            int year = campaignDeleteDto.getStart().getYear();
//            treeUpdateEventProducer.sendUpdateEvent();
        }
        return new HashMap<>();
    }

    @Transactional
    public Map<String,Integer> deleteCampaignDataByPeriodPublishRedis(CampaignDeleteDto campaignDeleteDto){
        //임계값 확인
        boolean checkThreshold = campaignDeleteDto.checkThreshold();
        Long memberId = memberRepository.findByEmail(campaignDeleteDto.getEmail())
                .orElseThrow(()-> new GrouException(ErrorCode.UNKNOWN_ERROR)).getId();
        String email  = campaignDeleteDto.getEmail();
        Map<LocalDate, AllCampaignTypeData> extractDeleteData = null;
        if(checkThreshold){
            extractDeleteData =
                    keywordCommandService.extractDeleteCampaignDataByPeriod(campaignDeleteDto.getStart(),campaignDeleteDto.getEnd(),campaignDeleteDto.getCampaignIds());
        }
        keywordCommandService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd());
        if(checkThreshold && extractDeleteData != null){
            int year = campaignDeleteDto.getStart().getYear();
            //Publish By Redis
            redisEventPublishService.publishUpdateEvent(email,year,extractDeleteData);
        }else{
            int year = campaignDeleteDto.getStart().getYear();
//            treeUpdateEventProducer.sendUpdateEvent();
        }
        return new HashMap<>();
    }
}
