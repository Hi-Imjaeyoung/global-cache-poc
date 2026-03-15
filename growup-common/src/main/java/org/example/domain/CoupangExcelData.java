package org.example.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
//@javax.persistence.MappedSuperclass; // JPA에서 부모 필드를 인식할 수 있게 만들어주는 어노테이션 Spring 3.x 부터는 사용 x
@jakarta.persistence.MappedSuperclass
public class CoupangExcelData {
    @Builder.Default
    private Long impressions = 0L;  // 노출수
    @Builder.Default
    private Long clicks = 0L;  // 클릭수
    @Builder.Default
    private Double clickRate = 0.0;  // 클릭률
    @Builder.Default
    private Long totalSales = 0L;  // 총 주문수
    @Builder.Default
    private Double cvr = 0.0;  // 전환율
    @Builder.Default
    private Double cpc = 0.0;  // CPC
    @Builder.Default
    private Double adCost = 0.0;  // 광고비
    @Builder.Default
    private Double adSales = 0.0;  // 광고매출
    @Builder.Default
    private Double roas = 0.0;  // ROAS

    private LocalDate date;  // 날짜

    public void update(CoupangExcelData coupangExcelData){
        clicks += coupangExcelData.getClicks();
        impressions += coupangExcelData.getImpressions();
        totalSales += coupangExcelData.getTotalSales();
        adCost += coupangExcelData.getAdCost();
        adSales += coupangExcelData.getAdSales();
        calculatePercentData();
    }

    public void calculatePercentData(){
        if(adCost != null && adCost != 0.0 && clicks != null ){
            cpc = (double) (Math.round(((adCost/ (double) clicks)*100)))/100.0  ;
        }else{
            cpc = 0.0;
        }
        if( adSales != null&& adSales != 0 && adCost != null){
            roas = (double) (Math.round((adSales/adCost)*10000)) / 100.0;
        }else{
            roas = 0.0;
        }
        if(clicks != null && clicks!=0 && impressions != null ) {
            clickRate = (double) (Math.round(((double)clicks/(double)impressions) * 10000))/100.0;
        }else{
            clickRate = 0.0;
        }
        if(totalSales!=null && totalSales !=0 && clicks !=null ){
            cvr = (double) (Math.round(((double)totalSales/(double)clicks)*10000)) /100.0;
        }else {
            cvr = 0.0;
        }

    }
}
