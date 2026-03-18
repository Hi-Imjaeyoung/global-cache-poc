package org.example.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.querydsl.core.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.example.domain.QCampaign.campaign;
import static org.example.domain.QKeyword.keyword;

@Service
@Slf4j
@AllArgsConstructor
public class LegacyLazySegmentTreeService {
    private final LongAdder hitCount = new LongAdder();
    private final LongAdder requestCount = new LongAdder();

    private static class SegmentNodeData {
        // key : My bitPacking
        Map<Integer, AllCampaignTypeData> dataMap = new ConcurrentHashMap<>();

        public boolean containsKey(int key) {
            return dataMap.containsKey(key);
        }

        public AllCampaignTypeData getCampaignAnalysisDto(int key) {
            return dataMap.get(key);
        }

        public void put(int key, AllCampaignTypeData data) {
            dataMap.put(key, data);
        }
    }

    private static class UserSegmentTree {
        // key : year
        Map<Integer, SegmentNodeData> treeMap = new ConcurrentHashMap<>();

        public SegmentNodeData get(int key) {
            return treeMap.get(key);
        }

        public void put(int key, SegmentNodeData data) {
            treeMap.put(key, data);
        }

        public boolean containsKey(int year) {
            return treeMap.containsKey(year);
        }

        public void remove(int year){treeMap.remove(year);}
    }

    // key : email
    private final Cache<String, UserSegmentTree> lazyCacheTree = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(5000)
            .build();

    private final Set<String> buildingInProgress = ConcurrentHashMap.newKeySet();

    public Long getCacheHitCount(){
        return hitCount.sum();
    }

    private SegmentNodeData getSegmentNodeDataSafe(String email, int year) {
        return lazyCacheTree
                .get(email, k -> new UserSegmentTree()) // 유저 없으면 생성
                .treeMap
                .computeIfAbsent(year, k -> new SegmentNodeData()); // 연도 없으면 생성
    }

    public int makeKey(int start, int end) {
        return (start << 9) | end;
    }

    public int convertLocalDateToCount(LocalDate localDate) {
        LocalDate startOfYear = LocalDate.of(localDate.getYear(), 1, 1);
        return (int) ChronoUnit.DAYS.between(startOfYear, localDate) + 1;
    }

    public LocalDate convertCountToLocalDate(int year, int count) {
        return LocalDate.of(year, 1, 1).plusDays(count - 1);
    }

    public boolean isTreeBuild(String email, int year){
        boolean isExist = lazyCacheTree.asMap().containsKey(email);
        if(isExist){
            UserSegmentTree value = lazyCacheTree.get(email,k-> new UserSegmentTree());
            return value.containsKey(year);
        }
        return false;
    }

    public AllCampaignTypeData getCachedOrSelectAllCampaignTypeDataByPeriod(String email,
                                                                    LocalDate start,
                                                                    LocalDate end){
        int nodeStart = 1;
        int nodeEnd = 365;
        if (start.isLeapYear()) nodeEnd++;
        int startCount = convertLocalDateToCount(start);
        int endCount = convertLocalDateToCount(end);
        if(start.getYear() == end.getYear()){
            return find(email,start.getYear(),nodeStart,nodeEnd,startCount,endCount);
        }
        AllCampaignTypeData preYear = find(email,start.getYear(),nodeStart,nodeEnd,startCount,nodeEnd);
        if(nodeEnd == 366 && !end.isLeapYear() ){
            nodeEnd--;
        }
        AllCampaignTypeData postYear = find(email,end.getYear(),nodeStart,nodeEnd,nodeStart,endCount);
        return  postYear.sum(preYear);
    }

    public AllCampaignTypeData find(String email, int year, int nodeStart, int nodeEnd, int targetStart, int targetEnd) {
        if (targetEnd < nodeStart || targetStart > nodeEnd) return new AllCampaignTypeData();
        if (targetStart <= nodeStart && nodeEnd <= targetEnd) {
            return getOrLoadNode(email,year, nodeStart, nodeEnd);
        }
        int mid = (nodeStart + nodeEnd) / 2;
        return find(email,year, nodeStart, mid, targetStart, targetEnd).sum(find(email,year, mid + 1, nodeEnd, targetStart, targetEnd));
    }

    private AllCampaignTypeData getOrLoadNode(String email, int year, int start, int end) {
        requestCount.increment();
        int key = makeKey(start, end);
        SegmentNodeData segmentNodeData = getSegmentNodeDataSafe(email,year);
        if (segmentNodeData.containsKey(key)) {
            hitCount.increment();
            log.debug("캐싱 hit! key : " + key);
            return segmentNodeData.getCampaignAnalysisDto(key);
        }else {
            log.error("트리 구조에 누락된 노드가 존재합니다. key : {}",key);
            return new AllCampaignTypeData();
        }
    }
    public void removeTreeBuildingKey(String email, int year){
        String key = email+":"+year;
        buildingInProgress.remove(key);
        return;
    }
    public boolean treeIsBuilding(String email,int year){
        String key = email+":"+year;
        return !buildingInProgress.add(key);
    }
    public void buildTree(String email, int year, List<Tuple> dbResult){
        SegmentNodeData segmentNodeData = getSegmentNodeDataSafe(email,year);
        Map<Integer, AllCampaignTypeData> dailyDataMap = mapDbResultToDailyData(dbResult);
        int nodeEnd = LocalDate.of(year,1,1).isLeapYear() ? 366 : 365;
        fillLeafNodes(segmentNodeData, dailyDataMap, 1, nodeEnd);
        buildTree(segmentNodeData, 1, nodeEnd);
    }

    private void fillLeafNodes(SegmentNodeData segmentNodeData, Map<Integer, AllCampaignTypeData> dailyDataMap, int start, int end) {
        for (int i = start; i <= end; i++) {
            int leafKey = makeKey(i, i);
            AllCampaignTypeData data = dailyDataMap.getOrDefault(leafKey, new AllCampaignTypeData());
            segmentNodeData.put(leafKey, data);
        }
    }
    private AllCampaignTypeData buildTree(SegmentNodeData segmentNodeData, int startCount, int endCount){
        if(startCount == endCount) {
            int key = makeKey(startCount,endCount);
            return segmentNodeData.getCampaignAnalysisDto(key);
        }

        int mid = (startCount + endCount)/2;
        AllCampaignTypeData leftValue = buildTree(segmentNodeData,startCount,mid);
        AllCampaignTypeData rightValue = buildTree(segmentNodeData,mid+1,endCount);

        int key = makeKey(startCount,endCount);
        AllCampaignTypeData sumValue = leftValue.add(rightValue);
        segmentNodeData.put(key,sumValue);
        return sumValue;
    }

    private  Map<Integer, AllCampaignTypeData> mapDbResultToDailyData(List<Tuple> dbResult){
        Map<Integer, AllCampaignTypeData> map = new HashMap<>();

        for (Tuple tuple : dbResult) {
            LocalDate date = tuple.get(keyword.date);

            int count = convertLocalDateToCount(date);
            int key = makeKey(count,count);

            String type = tuple.get(campaign.camAdType);
            Double cost = tuple.get(keyword.adCost.sum());
            Double sales = tuple.get(keyword.adSales.sum());
            double safeCost = (cost != null) ? cost : 0.0;
            double safeSales = (sales != null) ? sales : 0.0;

            AllCampaignTypeData newData = new AllCampaignTypeData(type, safeCost, safeSales);
            map.merge(key, newData, AllCampaignTypeData::add);
        }
        return map;
    }

    public Map<String, Long> getCacheStats() {
        return Map.of(
                "total", requestCount.sum(),
                "hit", hitCount.sum(),
                "miss", requestCount.sum() - hitCount.sum()
        );
    }

    public void resetCacheStats() {
        requestCount.reset();
        hitCount.reset();
    }

    public void incrementRequestCount(){
        requestCount.increment();;
    }

    public void reBuildTree(String email,LocalDate start, LocalDate end){
        // start와 end의 year이 같을때
        removeTreeDataByEmailAndYear(email,start.getYear());
        int startCount = convertLocalDateToCount(start);
        int endCount = convertLocalDateToCount(end);
        getOrLoadNode(email,start.getYear(),startCount,endCount);
    }
    public void removeTreeDataByEmailAndYear(String email, int year){
        // Map의 containsKey + get -> Cache의 getIfPresent
        UserSegmentTree userSegmentTree = lazyCacheTree.getIfPresent(email);
        if(userSegmentTree != null){
            userSegmentTree.remove(year);
        }
    }
    public void removeAllTreeDataByEmail(String email){
        lazyCacheTree.invalidate(email); // Map의 remove -> Cache의 invalidate
    }
    private SegmentNodeData getSegmentNodeData(String email, int year){
        UserSegmentTree userSegmentTree = lazyCacheTree.getIfPresent(email);
        if(userSegmentTree != null){
            return userSegmentTree.get(year);
        }
        return null;
    }
    public void updateTreeByPeriodData(String email, Map<LocalDate,AllCampaignTypeData> dataMap){
        for(LocalDate localDate : dataMap.keySet()){
            updateDay(email,localDate,dataMap.get(localDate));
        }
    }

    private void updateDay(String email,LocalDate changedDate, AllCampaignTypeData changedData){
        int year = changedDate.getYear();
        SegmentNodeData segmentNodeData = getSegmentNodeData(email,year);
        if(segmentNodeData == null){
            return;
        }
        int changedDateKey = convertLocalDateToCount(changedDate);
        int nodeEnd = changedDate.isLeapYear() ? 366 : 365;
        findAndUpdateTargetNode(segmentNodeData, 1, nodeEnd, changedDateKey, changedData);
    }

    private void findAndUpdateTargetNode(SegmentNodeData segmentNodeData,
                                       int rootS,
                                       int rootE,
                                       int target,
                                       AllCampaignTypeData changedData){
        if(rootS > target || rootE < target){
            return;
        }
        int key = makeKey(rootS,rootE);
        AllCampaignTypeData data = segmentNodeData.getCampaignAnalysisDto(key);
        if (data != null) {
            data.add(changedData);
        }
        if(rootS == rootE) return;
        int mid = (rootS + rootE)/2;
        findAndUpdateTargetNode(segmentNodeData,rootS,mid,target,changedData);
        findAndUpdateTargetNode(segmentNodeData,mid+1,rootE,target,changedData);
    }
}