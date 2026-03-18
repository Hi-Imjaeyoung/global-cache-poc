package org.example.service;

import com.querydsl.core.Tuple;
import lombok.AllArgsConstructor;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignAnalysisDto;
import org.example.repo.LegacyKeywordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.example.domain.QCampaign.campaign;
import static org.example.domain.QKeyword.keyword;

@Service
@AllArgsConstructor
public class LegacyKeywordService {

    private final LegacyKeywordRepository keywordRepository;
    public Map<LocalDate, AllCampaignTypeData>  extractDeleteCampaignDataByPeriod(LocalDate start,
                                                                                  LocalDate end,
                                                                                  List<Long> campaignIds){
        List<Tuple> queryResult =
                keywordRepository.getAllTypeOfCampaignAdCostSumAndAdSalesSumByPeriodAndCampaignIds(start,end,campaignIds);
        Map<LocalDate,AllCampaignTypeData> map = new HashMap<>();
        for(Tuple tuple : queryResult){
            LocalDate date = tuple.get(keyword.date);
            AllCampaignTypeData allCampaignTypeData = map.getOrDefault(date,new AllCampaignTypeData());
            String type = tuple.get(campaign.camAdType);
            Double cost = tuple.get(keyword.adCost.sum());
            Double sales = tuple.get(keyword.adSales.sum());
            AllCampaignTypeData oldData = new AllCampaignTypeData(type,cost,sales);
            allCampaignTypeData.sum(oldData);
        }
        return map;
    }


    public int deleteKeywordByCampaignIdsAndDate(List<Long> campaignIds,
                                                  LocalDate start,
                                                  LocalDate end){
        return keywordRepository.deleteKeywordByCampaignIdsAndDate(start,end,campaignIds);
    }

    public Map<String, CampaignAnalysisDto> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(String email,
                                                                                                      LocalDate start,
                                                                                                      LocalDate end){

        List<Tuple> queryResult = keywordRepository.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(start,end,email);
        Map<String,CampaignAnalysisDto> map = new HashMap<>();
        Double totalAdCost = 0.0;
        Double totalAdSale = 0.0;
        for(Tuple tuple: queryResult){
            Double adCostSum = tuple.get(keyword.adCost.sum());
            Double adSaleSum = tuple.get(keyword.adSales.sum());
            totalAdSale += adSaleSum;
            totalAdCost += adCostSum;
            map.put(tuple.get(campaign.camAdType),new CampaignAnalysisDto(adCostSum,adSaleSum));
        }
        map.put("총 매출",new CampaignAnalysisDto(totalAdCost,totalAdSale));
        return map;
    }

    public List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByList(String email,
                                                                                       LocalDate start,
                                                                                       LocalDate end){
        return keywordRepository.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(start,end,email);
    }
}
