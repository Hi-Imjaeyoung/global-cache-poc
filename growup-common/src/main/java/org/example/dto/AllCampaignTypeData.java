package org.example.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor // 빈 객체 생성을 위해 필요 (부모 노드 만들 때)
public class AllCampaignTypeData {

    // 타입별 통계를 담는 핵심 저장소
    private Map<String, CampaignAnalysisDto> campaignAnalysisDtoMap = new HashMap<>();

    private double maxAdCost = Double.MIN_VALUE,minAdCost  = Double.MAX_VALUE
            ,maxAdSales = Double.MIN_VALUE,minAdSales = Double.MAX_VALUE;

    // 1. 초기 생성자 (Leaf Node용 - 데이터 1건으로 시작)
    public AllCampaignTypeData(String type, double adCost, double adSales) {
        maxAdCost = adCost;
        maxAdSales = adSales;
        minAdCost = adCost;
        minAdSales = adSales;
        this.campaignAnalysisDtoMap.put(type, CampaignAnalysisDto.builder()
                .adCost(adCost)
                .adSales(adSales)
                .build());
    }
    public String toString(){
        return campaignAnalysisDtoMap.toString();
    }

    public AllCampaignTypeData(Map<String, CampaignAnalysisDto> externalMap) {
        this.campaignAnalysisDtoMap = new HashMap<>();
        if (externalMap == null || externalMap.isEmpty()) {
            return;
        }
        externalMap.forEach((key, dto) -> {
            this.campaignAnalysisDtoMap.put(key, CampaignAnalysisDto.builder().adSales(dto.getAdSales()).adCost(dto.getAdCost()).build());
        });
        for(String key:externalMap.keySet()){
            CampaignAnalysisDto now = externalMap.get(key);
            this.maxAdCost = Math.max(this.maxAdCost,now.getAdCost());
            this.minAdCost = Math.min(this.minAdCost,now.getAdCost());
            this.maxAdSales = Math.max(this.maxAdSales,now.getAdSales());
            this.minAdSales = Math.min(this.minAdSales,now.getAdSales());
        }
    }
    public AllCampaignTypeData add(AllCampaignTypeData other) {
        if (other == null || other.campaignAnalysisDtoMap.isEmpty()) {
            return this;
        }
        other.campaignAnalysisDtoMap.forEach((type, otherDto) -> {
            this.campaignAnalysisDtoMap.merge(type, otherDto, (oldVal, newVal) ->
                    oldVal.add(newVal.getAdCost(), newVal.getAdSales())
            );
        });
        maxAdSales = Math.max(this.maxAdSales,other.maxAdSales);
        maxAdCost = Math.max(this.maxAdCost,other.maxAdCost);
        minAdSales = Math.min(this.minAdSales, other.minAdSales);
        minAdCost = Math.min(this.minAdCost, other.minAdCost);
        return this;
    }

    public AllCampaignTypeData minus(AllCampaignTypeData other) {
        if (other == null || other.campaignAnalysisDtoMap.isEmpty()) {
            return this;
        }
        AllCampaignTypeData minusOne = new AllCampaignTypeData();
        minusOne.campaignAnalysisDtoMap = new HashMap<>(this.campaignAnalysisDtoMap);
        other.campaignAnalysisDtoMap.forEach((type, otherDto) -> {
            minusOne.campaignAnalysisDtoMap.merge(type, otherDto, (oldVal, newVal) ->
                    oldVal.minus(newVal.getAdCost(), newVal.getAdSales())
            );
        });
        return minusOne;
    }

    public AllCampaignTypeData sum(AllCampaignTypeData other){
        AllCampaignTypeData sumOne = new AllCampaignTypeData();
        sumOne.minAdSales = Math.min(this.minAdSales, other.minAdSales);
        sumOne.maxAdCost = Math.max(this.maxAdCost, other.maxAdCost);
        sumOne.minAdCost = Math.min(this.minAdCost, other.minAdCost);
        sumOne.maxAdSales = Math.max(this.maxAdSales, other.maxAdSales);
        sumOne.campaignAnalysisDtoMap = new HashMap<>(this.campaignAnalysisDtoMap);
        if (other == null || other.campaignAnalysisDtoMap.isEmpty()) {
            return sumOne;
        }
        other.campaignAnalysisDtoMap.forEach((type, otherDto) ->{
            sumOne.campaignAnalysisDtoMap.merge(type, otherDto, (oldVal, newVal) ->
                    oldVal.add(newVal.getAdCost(), newVal.getAdSales())
            );
        });
        return sumOne;
    }

    public void calculateMaxMin(){
        for(String key:campaignAnalysisDtoMap.keySet()){
            CampaignAnalysisDto campaignAnalysisDto = campaignAnalysisDtoMap.get(key);
            this.maxAdCost = Math.max(this.maxAdCost,campaignAnalysisDto.adCost);
            this.minAdCost = Math.min(this.minAdCost,campaignAnalysisDto.adCost);
            this.maxAdSales = Math.max(this.maxAdSales,campaignAnalysisDto.adSales);
            this.minAdSales = Math.min(this.maxAdSales,campaignAnalysisDto.adSales);
        }
    }
}