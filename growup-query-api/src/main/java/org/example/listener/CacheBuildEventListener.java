package org.example.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.example.exception.ErrorCode;
import org.example.exception.GrouException;
import org.example.config.CampaignRedisCacheManager;
import org.example.service.KeywordService;
import org.example.service.LazySegmentTreeService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheBuildEventListener {

    private final LazySegmentTreeService lazySegmentTreeService;
    private final KeywordService keywordService;
    private final CampaignRedisCacheManager campaignRedisCacheManager;
    private final Executor ioExecutor;  // ioExecutor 빈도 주입받아야 함!
    private final Executor cpuExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTreeBuildEvent(TreeBuildEvent event) {
        String email = event.getEmail();
        int year = event.getYear();
        if (lazySegmentTreeService.treeIsBuilding(email, year)) {
            log.info("Tree is Already Building! email: {}, year: {}", email, year);
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            AllCampaignTypeData[] savedData = campaignRedisCacheManager.getCachedTreeData(email, year);
            if (savedData != null) {
                log.debug("Redis 캐시 히트! 트리 인메모리 적재");
                lazySegmentTreeService.saveBackupTreeData(email,year,savedData);
                return null;
            }
            log.debug("Redis 캐시 미스! DB 원본 조회 시작.");
            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = LocalDate.of(year, 12, 31);
            return keywordService
                    .getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByDate(startDate, endDate, email);
            }, CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS, ioExecutor))
            .thenAcceptAsync(data -> {
                if (data == null) return;
                log.debug("메모리 세그먼트 트리 빌드 시작...");
                AllCampaignTypeData[] segTree = lazySegmentTreeService.startBuildTree(email, year, data);
                log.debug("트리 빌드 완료!");
                log.debug("현재 트리 메모리 크기는 {}",lazySegmentTreeService.getTreeMemory(email));
                campaignRedisCacheManager.saveRawData(email,year,segTree);
            }, cpuExecutor)
            .whenComplete((result,ex) ->{
                lazySegmentTreeService.removeTreeBuildingKey(email, year);
                if (ex != null) {
                    log.error("비동기 트리 빌드 파이프라인에서 치명적 에러 발생! [email: {}]", email, ex);
                    throw new GrouException(ErrorCode.UNKNOWN_ERROR);
                }
            });
    }
}
