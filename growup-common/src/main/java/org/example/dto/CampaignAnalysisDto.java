package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CampaignAnalysisDto {
    Double adCost;
    Double adSales;
    String campAdType;
    Long campaignId;
    public CampaignAnalysisDto(Double adCost, Double adSales){
        this.adCost = adCost;
        this.adSales = adSales;
    }
    public void add(CampaignAnalysisDto other){
        this.adSales += other.adSales;
        this.adCost += other.adCost;
    }
    public CampaignAnalysisDto add(double adCost,double adSales){
        return new CampaignAnalysisDto(
                this.adCost + adCost,
                this.adSales + adSales
        );
    }

    public CampaignAnalysisDto minus(double adCost,double adSales){
        return new CampaignAnalysisDto(
                this.adCost - adCost,
                this.adSales - adSales
        );
    }
    public void plusAdCost(Double adCost){
        this.adCost += adCost;
    }

    public void plusAdSales (Double adSales){
        this.adSales += adSales;
    }
}
