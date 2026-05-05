package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.AllCampaignTypeData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class SerializationSizeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Map vs Array 직렬화 크기 비교")
    void compareSerializationSize() throws Exception {
        int n = 1500; // 벤치마크와 동일한 데이터 개수
        AllCampaignTypeData[] arrayTree = new AllCampaignTypeData[n];
        Map<Integer, AllCampaignTypeData> mapTree = new HashMap<>();

        // 1. 데이터 채우기 (JMH와 동일한 객체 생성 로직)
        for (int i = 0; i < n; i++) {
            AllCampaignTypeData data = getDefaultObj();
            arrayTree[i] = data;
            mapTree.put(i, data);
        }

        // 2. 직렬화 수행
        byte[] arrayBytes = mapper.writeValueAsBytes(arrayTree);
        byte[] mapBytes = mapper.writeValueAsBytes(mapTree);

        // 3. 결과 출력
        System.out.println("========================================");
        System.out.println("📊 직렬화 페이로드 비교 결과 (N=" + n + ")");
        System.out.println("----------------------------------------");
        System.out.printf("🔹 Array Size: %d bytes (%.2f KB)\n",
                arrayBytes.length, arrayBytes.length / 1024.0);
        System.out.printf("🔹 Map Size  : %d bytes (%.2f KB)\n",
                mapBytes.length, mapBytes.length / 1024.0);

        double reduction = (1 - (double) arrayBytes.length / mapBytes.length) * 100;
        System.out.println("----------------------------------------");
        System.out.printf("✅ 용량 절감률: %.2f%%\n", reduction);
        System.out.println("========================================");
    }

    private AllCampaignTypeData getDefaultObj() {
        AllCampaignTypeData data1 = new AllCampaignTypeData("testType", 13123.0, 12323.0);
        AllCampaignTypeData data2 = new AllCampaignTypeData("testType2", 123123.2, 121.2);
        return data1.add(data2);
    }
}