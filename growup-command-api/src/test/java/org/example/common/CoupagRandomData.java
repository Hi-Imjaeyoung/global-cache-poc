package org.example.common;

import org.example.domain.CoupangExcelData;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CoupagRandomData extends CoupangExcelData {
    static final String KEYWORD = "keyword";
    static final String OPTION = "option";
    static Random random = new Random();
    public static CoupangExcelData createRandomData(){
        CoupangExcelData coupangExcelData = CoupangExcelData.builder()
                .impressions(random.nextLong(100000)) // 0 ~ 99,999 사이의 Long 값
                .clicks(random.nextLong(5000))      // 0 ~ 4,999 사이의 Long 값
                .totalSales(random.nextLong(100))    // 0 ~ 99 사이의 Long 값
                .adCost((double)random.nextLong(1000000)) // 0.0 ~ 1,000,000.0 사이의 Double 값
                .adSales((double)random.nextLong(5000000)) // 0.0 ~ 5,000,000.0 사이의 Double 값
                .build();
        coupangExcelData.calculatePercentData();
        return coupangExcelData;
    }

    public static String createRandomKeyword(){
        return KEYWORD + random.nextLong(500);
    }

    public static Map<String,Long> createKeyProductSales(Long totalSales){
        Map<String,Long> map = new HashMap<>();
        map.put(OPTION + random.nextLong(10),totalSales);
        return map;
    }

}
