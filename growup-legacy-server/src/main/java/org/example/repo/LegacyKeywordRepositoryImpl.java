package org.example.repo;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import static org.example.domain.QCampaign.campaign;
import static org.example.domain.QKeyword.keyword;
import static org.example.domain.QMember.member;


@RequiredArgsConstructor
public class LegacyKeywordRepositoryImpl implements LegacyKeywordRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(LocalDate start, LocalDate end, String email){
        var dateBetween = keyword.date.between(
                start,
                end
        );
        return queryFactory.select(keyword.date, campaign.camAdType, keyword.adCost.sum(), keyword.adSales.sum())
                            .from(keyword)
                            .join(keyword.campaign, campaign)
                            .join(campaign.member, member)
                            .where(member.email.eq(email),
                                    dateBetween
                            )
                            .groupBy(keyword.date ,campaign.camAdType)
                            .fetch();
    }

    @Override
    public List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(LocalDate start, LocalDate end, String email){
        var dateBetween = keyword.date.between(
                start,
                end
        );
        return queryFactory.select(campaign.camAdType, keyword.adCost.sum(), keyword.adSales.sum())
                .from(keyword)
                .join(keyword.campaign, campaign)
                .join(campaign.member, member)
                .where(member.email.eq(email),
                        dateBetween
                )
                .groupBy(campaign.camAdType)
                .fetch();
    }

    public int deleteKeywordByCampaignIdsAndDate(LocalDate start,
                                                 LocalDate end,
                                                 List<Long> campaignIds){
        var dateBetween = keyword.date.between(start,end);
        long deletedCount = queryFactory.delete(keyword)
                .where(
                        dateBetween,
                        keyword.campaign.id.in(campaignIds)
                )
                .execute();
        return (int) deletedCount;
    }

    public List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSalesSumByPeriodAndCampaignIds(LocalDate start,
                                                                                        LocalDate end,
                                                                                        List<Long> campaignIds){
        var dateBetween = keyword.date.between(start,end);
        return queryFactory
                .select(keyword.date,campaign.camAdType,keyword.adCost.sum(),keyword.adSales.sum())
                .from(keyword)
                .join(keyword.campaign,campaign)
                .where(dateBetween,campaign.id.in(campaignIds))
                .groupBy(campaign.camAdType,keyword.date)
                .fetch();
    }
}
