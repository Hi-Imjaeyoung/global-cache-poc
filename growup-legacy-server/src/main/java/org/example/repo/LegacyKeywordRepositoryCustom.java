package org.example.repo;

import com.querydsl.core.Tuple;

import java.time.LocalDate;
import java.util.List;

public interface LegacyKeywordRepositoryCustom {

    List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(LocalDate start, LocalDate end, String email);

    List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(LocalDate start, LocalDate end, String email);

    int deleteKeywordByCampaignIdsAndDate(LocalDate start, LocalDate end,List<Long> campaignIds);

    List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSalesSumByPeriodAndCampaignIds(LocalDate start, LocalDate end, List<Long> campaignIds);
}
