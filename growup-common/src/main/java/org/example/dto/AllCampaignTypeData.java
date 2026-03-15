package org.example.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor // 빈 객체 생성을 위해 필요 (부모 노드 만들 때)
public class AllCampaignTypeData {

    // 타입별 통계를 담는 핵심 저장소
    private Map<String, CampaignAnalysisDto> campaignAnalysisDtoMap = new HashMap<>();

    // 1. 초기 생성자 (Leaf Node용 - 데이터 1건으로 시작)
    public AllCampaignTypeData(String type, double adCost, double adSales) {
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
        return this;
    }

    public AllCampaignTypeData sum(AllCampaignTypeData other){
        AllCampaignTypeData sumOne = new AllCampaignTypeData();
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

    public void calculateTotalData(){

    }
}