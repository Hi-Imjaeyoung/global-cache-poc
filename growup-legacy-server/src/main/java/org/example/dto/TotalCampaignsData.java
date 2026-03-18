package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TotalCampaignsData {
    //key : campAdType
    Map<String,CampaignAnalysisDto> sumOfAdSalesAndAdCostByCampaignType;

    //key : campaignName
    Map<String,CampaignAnalysisDto> adSalesAndAdCostByCampaignName;
}
