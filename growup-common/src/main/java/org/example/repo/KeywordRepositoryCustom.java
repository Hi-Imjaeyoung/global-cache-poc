package org.example.repo;

import com.querydsl.core.Tuple;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignAnalysisDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface KeywordRepositoryCustom {

    Map<Integer, AllCampaignTypeData> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(LocalDate start, LocalDate end, String email);
    Map<String, CampaignAnalysisDto> getAllTypeOfCampaignAdCostAndSaleSumByCampaignType(LocalDate start, LocalDate end, String email);
}
