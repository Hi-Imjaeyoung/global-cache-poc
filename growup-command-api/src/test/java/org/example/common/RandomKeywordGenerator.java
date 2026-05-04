package org.example.common;



import org.example.domain.CoupangExcelData;
import org.example.domain.Keyword;
import org.example.dto.KeywordDto;

import java.util.HashMap;

public class RandomKeywordGenerator {
    Long campaignId;
    RandomDateGenerator randomDateGenerator;
    public RandomKeywordGenerator(Long campaignId, RandomDateGenerator randomDateGenerator){
        this.campaignId = campaignId;
        this.randomDateGenerator = randomDateGenerator;
    }
    public KeywordDto makeNonSearchKeyword(CoupangExcelData coupangExcelData) {
        return KeywordDto.builder()
                .adCost(coupangExcelData.getAdCost())
                .cpc(coupangExcelData.getCpc())
                .campaignId(campaignId)
                .cvr(coupangExcelData.getCvr())
                .adSales(coupangExcelData.getAdSales())
                .roas(coupangExcelData.getRoas())
                .impressions(coupangExcelData.getImpressions())
                .clicks(coupangExcelData.getClicks())
                .date(randomDateGenerator.getLocalDate())
                .keyword("-")
                .adType("비 검색 영역")
                .clickRate(coupangExcelData.getClickRate())
                .totalSales(coupangExcelData.getTotalSales())
                .build();
    };
    public  KeywordDto makeSearchKeyword(CoupangExcelData coupangExcelData){
        return KeywordDto.builder()
                .adCost(coupangExcelData.getAdCost())
                .cpc(coupangExcelData.getCpc())
                .campaignId(campaignId)
                .cvr(coupangExcelData.getCvr())
                .adSales(coupangExcelData.getAdSales())
                .roas(coupangExcelData.getRoas())
                .impressions(coupangExcelData.getImpressions())
                .clicks(coupangExcelData.getClicks())
                .date(randomDateGenerator.getLocalDate())
                .keyword(CoupagRandomData.createRandomKeyword())
                .adType("검색 영역")
                .clickRate(coupangExcelData.getClickRate())
                .totalSales(coupangExcelData.getTotalSales())
                .build();
    };
}
