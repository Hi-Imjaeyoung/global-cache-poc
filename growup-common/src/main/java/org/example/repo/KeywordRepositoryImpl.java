package org.example.repo;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignAnalysisDto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.example.domain.QCampaign.campaign;
import static org.example.domain.QKeyword.keyword;
import static org.example.domain.QMember.member;

@RequiredArgsConstructor
@Slf4j
public class KeywordRepositoryImpl implements KeywordRepositoryCustom{

    private final JPAQueryFactory queryFactory;


    public int convertLocalDateToCount(LocalDate localDate) {
        LocalDate startOfYear = LocalDate.of(localDate.getYear(), 1, 1);
        return (int) ChronoUnit.DAYS.between(startOfYear, localDate) + 1;
    }

    @Override
    public AllCampaignTypeData[] getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(LocalDate start, LocalDate end, String email){
        var dateBetween = keyword.date.between(start, end);

        List<Tuple> dbResult = queryFactory.select(keyword.date, campaign.camAdType, keyword.adCost.sum(), keyword.adSales.sum())
                .from(keyword)
                .join(keyword.campaign, campaign)
                .join(campaign.member, member)
                .where(member.email.eq(email), dateBetween)
                .groupBy(keyword.date, campaign.camAdType)
                .orderBy(keyword.date.asc())
                .fetch();

        AllCampaignTypeData[] arr = new AllCampaignTypeData[370];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new AllCampaignTypeData();
        }
        for (Tuple tuple : dbResult) {
            LocalDate date = tuple.get(keyword.date);
            int count = convertLocalDateToCount(date);
            String type = tuple.get(campaign.camAdType);
            Double cost = tuple.get(keyword.adCost.sum());
            Double sales = tuple.get(keyword.adSales.sum());
            double safeCost = (cost != null) ? cost : 0.0;
            double safeSales = (sales != null) ? sales : 0.0;
            AllCampaignTypeData newData = new AllCampaignTypeData(type, safeCost, safeSales);
            arr[count].add(newData);
        }
        return arr;
    }
    @Override
    public Map<String, CampaignAnalysisDto> getAllTypeOfCampaignAdCostAndSaleSumByCampaignType(LocalDate start, LocalDate end, String email){
        var dateBetween = keyword.date.between(
                start,
                end
        );
        List<Tuple> queryResult = queryFactory.select(campaign.camAdType, keyword.adCost.sum(), keyword.adSales.sum())
                .from(keyword)
                .join(keyword.campaign, campaign)
                .join(campaign.member, member)
                .where(member.email.eq(email),
                        dateBetween
                )
                .groupBy(campaign.camAdType)
                .fetch();
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

    public Map<LocalDate, AllCampaignTypeData> getDeletedDataByPeriodInCampaignIds(LocalDate start, LocalDate end, List<Long> campaignIds){
        var dateBetween = keyword.date.between(start,end);
        List<Tuple> queryResult = queryFactory
                .select(keyword.date,campaign.camAdType,keyword.adCost.sum(),keyword.adSales.sum())
                .from(keyword)
                .join(keyword.campaign,campaign)
                .where(dateBetween,campaign.id.in(campaignIds))
                .groupBy(keyword.date,campaign.camAdType)
                .fetch();
        Map<LocalDate,AllCampaignTypeData> map = new HashMap<>();
        for(Tuple tuple : queryResult){
            LocalDate date = tuple.get(keyword.date);
            String type = tuple.get(campaign.camAdType);
            Double cost = tuple.get(keyword.adCost.sum());
            Double sales = tuple.get(keyword.adSales.sum());
            AllCampaignTypeData oldData = new AllCampaignTypeData(type,cost,sales);
            if(map.containsKey(date)){
                map.get(date).add(oldData);
            }else{
                map.put(date,oldData);
            }
        }
        return map;
    }

    public void deleteKeywordByCampaignIdsAndDate(List<Long>campaignIds,LocalDate start,LocalDate end){
        var dateBetween = keyword.date.between(start,end);
        long deletedCount = queryFactory.delete(keyword)
                .where(
                        dateBetween,
                        keyword.campaign.id.in(campaignIds)
                )
                .execute();
        log.info("캠페인 데이터 삭제 완료: {} 건 삭제됨", deletedCount);
    }
}
