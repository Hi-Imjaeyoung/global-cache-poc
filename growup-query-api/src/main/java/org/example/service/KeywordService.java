package org.example.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignAnalysisDto;
import org.example.repo.KeywordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class KeywordService {
    private final KeywordRepository keywordRepository;

    public AllCampaignTypeData[] getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByDate(LocalDate start, LocalDate end, String email){
        return keywordRepository.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(start,end,email);
    }

    public Map<String, CampaignAnalysisDto> getAllTypeOfCampaignAdCostAndSaleSumByCampaignType(LocalDate start, LocalDate end, String email){
        return keywordRepository.getAllTypeOfCampaignAdCostAndSaleSumByCampaignType(start,end,email);
    }

}
