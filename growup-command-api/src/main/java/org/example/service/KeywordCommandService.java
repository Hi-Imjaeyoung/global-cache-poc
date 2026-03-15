package org.example.service;

import lombok.AllArgsConstructor;
import org.example.dto.AllCampaignTypeData;
import org.example.repo.KeywordRepository;
import org.example.repo.KeywordRepositoryCustom;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class KeywordCommandService {

    private final KeywordRepository keywordRepository;

    public Map<LocalDate, AllCampaignTypeData> extractDeleteCampaignDataByPeriod(LocalDate start, LocalDate end, List<Long> campaignIds){
        return keywordRepository.getDeletedDataByPeriodInCampaignIds(start,end,campaignIds);
    }
}
