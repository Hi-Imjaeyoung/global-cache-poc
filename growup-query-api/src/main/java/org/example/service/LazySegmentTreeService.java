package org.example.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.openjdk.jol.info.GraphLayout;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

@Service
@Slf4j
@AllArgsConstructor
public class LazySegmentTreeService {
    private static class UserSegmentTree {
        // key : year
        Map<Integer, AllCampaignTypeData[]> treeMap = new ConcurrentHashMap<>();

        public AllCampaignTypeData[] get(int key) {
            return treeMap.get(key);
        }
        public boolean containsKey(int year) {
            return treeMap.containsKey(year);
        }
        public void remove(int year){treeMap.remove(year);}
    }

    // key : email
    private final Cache<String, UserSegmentTree> lazyCacheTree = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(500)
            .build();

    private final Set<String> buildingInProgress = ConcurrentHashMap.newKeySet();

    private AllCampaignTypeData[] getSegmentTreeSafe(String email, int year) {
        return lazyCacheTree
                .get(email, k -> new UserSegmentTree()) // 유저 없으면 생성
                .treeMap
                .computeIfAbsent(year, k -> new AllCampaignTypeData[370*4]); // 연도 없으면 생성
    }

    public int convertLocalDateToCount(LocalDate localDate) {
        return localDate.getDayOfYear();
    }

    public boolean isTreeBuild(String email, int year){
        boolean isExist = lazyCacheTree.asMap().containsKey(email);
        if(isExist){
            UserSegmentTree value = lazyCacheTree.get(email,k-> new UserSegmentTree());
            return value.containsKey(year);
        }
        return false;
    }
    public void removeAllTreeData(){
        lazyCacheTree.invalidateAll();
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
            AllCampaignTypeData[] tree = lazyCacheTree.asMap().get(email).get(start.getYear());
            return findNode(tree,1,nodeStart,nodeEnd,startCount,endCount);
        }
        AllCampaignTypeData[] tree = lazyCacheTree.asMap().get(email).get(start.getYear());
        AllCampaignTypeData preYear = findNode(tree,1,nodeStart,nodeEnd,startCount,nodeEnd);
        if(nodeEnd == 366 && !end.isLeapYear() ){
            nodeEnd--;
        }
        tree = lazyCacheTree.asMap().get(email).get(end.getYear());
        AllCampaignTypeData postYear = findNode(tree,1,nodeStart,nodeEnd,nodeStart,endCount);
        return  postYear.sum(preYear);
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

    public AllCampaignTypeData[] startBuildTree(String email, int year, AllCampaignTypeData[] rawData){
        removeTreeDataByEmailAndYear(email,year);
        AllCampaignTypeData[] tree = getSegmentTreeSafe(email,year);
        int start = 1;
        int end = 365;
        if(LocalDate.of(year,1,1).isLeapYear()) end++;
        buildTree(tree,rawData,1,start,end);
        return tree;
    }

    // arr 구조로 변경된 트리 빌드
    public AllCampaignTypeData buildTree(AllCampaignTypeData[] tree,
                                         AllCampaignTypeData[] rawData,
                                         int node,int start, int end){
        if(start == end){
            return tree[node] = rawData[start];
        }
        int mid = (start + end) / 2;
        AllCampaignTypeData left = buildTree(tree,rawData,node*2,start,mid);
        AllCampaignTypeData right = buildTree(tree,rawData,(node*2)+1,mid+1,end);
        return tree[node] = left.sum(right);
    }

    public AllCampaignTypeData findNode(AllCampaignTypeData[] tree,
                                        int node,int start, int end,
                                        int targetS, int targetE){
        if (targetE < start || targetS > end) return new AllCampaignTypeData();
        if (targetS <= start && end <= targetE) {
            return tree[node];
        }
        int mid = (start + end) / 2;
        return findNode(tree,2*node,start,mid,targetS,targetE)
                .sum(findNode(tree,(node*2)+1,mid+1,end,targetS,targetE));
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
    private AllCampaignTypeData[] getSegmentNodeData(String email, int year){
        UserSegmentTree userSegmentTree = lazyCacheTree.getIfPresent(email);
        if(userSegmentTree != null){
            return userSegmentTree.get(year);
        }
        return null;
    }
    public void updateTreeByPeriodData(AllCampaignTypeData[] oldData, Map<LocalDate,AllCampaignTypeData> dataMap){
        for(LocalDate localDate : dataMap.keySet()){
            updateDay(oldData,localDate,dataMap.get(localDate));
        }
    }

    private void updateDay(AllCampaignTypeData[] oldData,LocalDate changedDate, AllCampaignTypeData changedData){
        int year = changedDate.getYear();
        int changedDateKey = convertLocalDateToCount(changedDate);
        int nodeEnd = changedDate.isLeapYear() ? 366 : 365;
        findAndUpdateTargetNode(oldData,1, 1, nodeEnd, changedDateKey, changedData);
    }

    private void findAndUpdateTargetNode(AllCampaignTypeData[] tree,
                                         int node,int rootS,int rootE,int target,
                                         AllCampaignTypeData changedData){
        if(rootS > target || rootE < target){
            return;
        }
        AllCampaignTypeData data = tree[node];
        if (data != null) {
            data.minus(changedData);
        }
        if(rootS == rootE) return;
        int mid = (rootS + rootE)/2;
        findAndUpdateTargetNode(tree,node*2,rootS,mid,target,changedData);
        findAndUpdateTargetNode(tree,(node*2)+1,mid+1,rootE,target,changedData);
    }

    public long getTreeMemory(String email){
//        System.out.println(GraphLayout.parseInstance(lazyCacheTree).toFootprint());
        UserSegmentTree tree = lazyCacheTree.asMap().get(email);
        return GraphLayout.parseInstance(tree).totalSize();
    }

    public void saveBackupTreeData(String email, int year, AllCampaignTypeData[] savedTreeData){
        if(savedTreeData == null) return;
        UserSegmentTree value = lazyCacheTree.get(email,k-> new UserSegmentTree());
        value.treeMap.put(year,savedTreeData);
    }
}