package org.example.repo;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.dto.AllCampaignTypeData;
import org.example.dto.CampaignAnalysisDto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.example.domain.QCampaign.campaign;
import static org.example.domain.QKeyword.keyword;
import static org.example.domain.QMember.member;

@RequiredArgsConstructor
public class KeywordRepositoryImpl implements KeywordRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public int makeKey(int start, int end) {
        return (start << 9) | end;
    }

    public int convertLocalDateToCount(LocalDate localDate) {
        LocalDate startOfYear = LocalDate.of(localDate.getYear(), 1, 1);
        return (int) ChronoUnit.DAYS.between(startOfYear, localDate) + 1;
    }

    @Override
    public Map<Integer, AllCampaignTypeData> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(LocalDate start, LocalDate end, String email){
        var dateBetween = keyword.date.between(
                start,
                end
        );
        List<Tuple> dbResult = queryFactory.select(keyword.date, campaign.camAdType, keyword.adCost.sum(), keyword.adSales.sum())
                            .from(keyword)
                            .join(keyword.campaign, campaign)
                            .join(campaign.member, member)
                            .where(member.email.eq(email),
                                    dateBetween
                            )
                            .groupBy(keyword.date ,campaign.camAdType)
                            .fetch();

        Map<Integer, AllCampaignTypeData> map = new HashMap<>();

        for (Tuple tuple : dbResult) {
            LocalDate date = tuple.get(keyword.date);

            int count = convertLocalDateToCount(date);
            int key = makeKey(count,count);

            String type = tuple.get(campaign.camAdType);
            Double cost = tuple.get(keyword.adCost.sum());
            Double sales = tuple.get(keyword.adSales.sum());
            double safeCost = (cost != null) ? cost : 0.0;
            double safeSales = (sales != null) ? sales : 0.0;

            AllCampaignTypeData newData = new AllCampaignTypeData(type, safeCost, safeSales);
            map.merge(key, newData, AllCampaignTypeData::add);
        }
        return map;
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
}
