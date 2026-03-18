package org.example.repo;

import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignAnalysisDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface KeywordRepositoryCustom {

    AllCampaignTypeData[] getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(LocalDate start, LocalDate end, String email);
    Map<String, CampaignAnalysisDto> getAllTypeOfCampaignAdCostAndSaleSumByCampaignType(LocalDate start, LocalDate end, String email);
    Map<LocalDate,AllCampaignTypeData> getDeletedDataByPeriodInCampaignIds(LocalDate start,LocalDate end, List<Long> campaignIds);
    void deleteKeywordByCampaignIdsAndDate(List<Long>campaignIds,LocalDate start,LocalDate end);
}
