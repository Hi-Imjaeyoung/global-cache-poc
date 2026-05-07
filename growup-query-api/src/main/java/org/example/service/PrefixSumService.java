package org.example.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@AllArgsConstructor
public class PrefixSumService {
    private static class UserSegmentTree {
        // key : year
        Map<Integer, AllCampaignTypeData[]> prefixMap = new ConcurrentHashMap<>();

        public AllCampaignTypeData[] get(int key) {
            return prefixMap.get(key);
        }
        public boolean containsKey(int year) {
            return prefixMap.containsKey(year);
        }
        public void remove(int year){prefixMap.remove(year);}
    }

    private final Cache<String, UserSegmentTree> prefixSumCache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(500)
            .build();
    private final Set<String> buildingInProgress = ConcurrentHashMap.newKeySet();

    public void saveBackupPrefixData(String email, int year, AllCampaignTypeData[] savedData){
        if(savedData == null) return;
        UserSegmentTree value = prefixSumCache.get(email,k->new UserSegmentTree());
        value.prefixMap.put(year,savedData);
    }

    public void removeAllPrefixData(){
        prefixSumCache.invalidateAll();
    }

    public boolean hasPrefixData(String email, int year){
        boolean isExist = prefixSumCache.asMap().containsKey(email);
        if(isExist){
            UserSegmentTree value = prefixSumCache.get(email, k-> new UserSegmentTree());
            return value.containsKey(year);
        }
        return false;
    }

    public AllCampaignTypeData getCachedOrSelectAllCampaignTypeDataByPeriod(String email, LocalDate start, LocalDate end){
        int year = start.getYear();
        int startCnt = start.getDayOfYear();
        int endCnt = end.getDayOfYear();
        UserSegmentTree value = prefixSumCache.get(email,k->new UserSegmentTree());
        AllCampaignTypeData[] prefixArr = value.prefixMap.get(year);
        AllCampaignTypeData result;
        if(startCnt == 1){
            result = prefixArr[endCnt];
        }else{
            result = prefixArr[endCnt].minus(prefixArr[startCnt-1]);
        }
        findMaxAndMin(prefixArr,result,startCnt,endCnt);
        return result;
    }

    public void findMaxAndMin(AllCampaignTypeData[] arr, AllCampaignTypeData result, int start,int end){
        AllCampaignTypeData startData = arr[start].minus(arr[start-1]);
        startData.calculateMaxMin();
        double maxAdCost = startData.getMaxAdCost();
        double minAdCost = startData.getMinAdCost();
        double maxAdSales = startData.getMaxAdSales();
        double minAdSales = startData.getMinAdSales();
        for(int i=start+1;i<=end;i++){
            AllCampaignTypeData now = arr[i].minus(arr[i-1]);
            now.calculateMaxMin();
            maxAdCost = Math.max(maxAdCost,now.getMaxAdCost());
            minAdSales = Math.min(minAdSales,now.getMinAdSales());
            maxAdSales = Math.max(maxAdSales,now.getMaxAdSales());
            minAdCost = Math.min(minAdCost,now.getMinAdCost());
        }
        result.setMaxAdSales(maxAdSales);
        result.setMaxAdCost(maxAdCost);
        result.setMinAdCost(minAdCost);
        result.setMinAdSales(minAdSales);
    }

    public boolean prefixIsBuilding(String email, int year){
        String key = email+":"+year;
        return !buildingInProgress.add(key);
    }

    public void removePrefixDataByEmailAndYear(String email, int year){
        UserSegmentTree userSegmentTree = prefixSumCache.getIfPresent(email);
        if(userSegmentTree != null){
            userSegmentTree.remove(year);
        }
    }

    private AllCampaignTypeData[] getEmptyArrSafe(String email, int year) {
        return prefixSumCache
                .get(email, k -> new UserSegmentTree()) // 유저 없으면 생성
                .prefixMap
                .computeIfAbsent(year, k -> new AllCampaignTypeData[370]); // 연도 없으면 생성
    }

    public AllCampaignTypeData[] startBuildPrefix(String email, int year, AllCampaignTypeData[] data){
        removePrefixDataByEmailAndYear(email,year);
        AllCampaignTypeData[] tree = getEmptyArrSafe(email,year);
        tree[0] = new AllCampaignTypeData(); // 기본 생성자나 0점 세팅 메서드 사용
        int end = 365;
        tree[1] = new AllCampaignTypeData();
        tree[1].add(data[1]);
        if(LocalDate.of(year,1,1).isLeapYear()) end++;
        for(int i=2;i<=end;i++){
            tree[i] = tree[i-1].sum(data[i]);
        }
        return tree;
    }

    public void removeTreeBuildingKey(String email, int year){
        String key = email+":"+year;
        buildingInProgress.remove(key);
    }
}
