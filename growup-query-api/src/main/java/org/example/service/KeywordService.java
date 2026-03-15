package org.example.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignAnalysisDto;
import org.example.repo.KeywordRepositoryCustom;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class KeywordService {
    private final KeywordRepositoryCustom keywordRepositoryCustom;

    public Map<Integer, AllCampaignTypeData> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByDate(LocalDate start, LocalDate end, String email){
        return keywordRepositoryCustom.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(start,end,email);
    }

    public Map<String, CampaignAnalysisDto> getAllTypeOfCampaignAdCostAndSaleSumByCampaignType(LocalDate start, LocalDate end, String email){
        return keywordRepositoryCustom.getAllTypeOfCampaignAdCostAndSaleSumByCampaignType(start,end,email);
    }

}
