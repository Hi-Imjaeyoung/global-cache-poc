package com.example.benchmark;

import org.example.dto.AllCampaignTypeData;
import org.openjdk.jmh.annotations.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime) // 평균 응답 시간 측정
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SerializationBench{
    private ObjectMapper mapper;
    private AllCampaignTypeData[] arrayTree;
    private Map<Integer, AllCampaignTypeData> mapTree;

    @Setup(Level.Trial) // 테스트 시작 전 한 번만 실행
    public void setup() {
        mapper = new ObjectMapper(); // 초기화 필수!
        int n = 1500; // 1년 365일 데이터를 담기 위한 세그먼트 트리 배열 크기
        arrayTree = new AllCampaignTypeData[n];
        mapTree = new HashMap<>();
        AllCampaignTypeData allCampaignTypeData  = getDefaultObj();
        for(int i=0;i<n;i++){
            arrayTree[i] = allCampaignTypeData;
            mapTree.put(i,allCampaignTypeData);
        }
    }

    private AllCampaignTypeData getDefaultObj(){

        AllCampaignTypeData allCampaignTypeData = new AllCampaignTypeData("testType",13123.0,12323.0);
        AllCampaignTypeData allCampaignTypeData1 = new AllCampaignTypeData("testType2",123123.2,121.2);
        return allCampaignTypeData.add(allCampaignTypeData1);
    }

    @Benchmark
    public byte[] arraySerialization() throws Exception {
        return mapper.writeValueAsBytes(arrayTree);
    }

    @Benchmark
    public byte[] mapSerialization() throws Exception {
        return mapper.writeValueAsBytes(mapTree);
    }
}