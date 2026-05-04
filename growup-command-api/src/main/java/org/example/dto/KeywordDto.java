package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KeywordDto {
    String keyword;
    String adType;
    Long campaignId;
    LocalDate date;
    private Long impressions = 0L;  // 노출수
    private Long clicks = 0L;  // 클릭수
    private Double clickRate = 0.0;  // 클릭률
    private Long totalSales = 0L;  // 총 주문수
    private Double cvr = 0.0;  // 전환율
    private Double cpc = 0.0;  // CPC
    private Double adCost = 0.0;  // 광고비
    private Double adSales = 0.0;  // 광고매출
    private Double roas = 0.0;  // ROAS
}
