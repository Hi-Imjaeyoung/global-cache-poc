package org.example.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.example.exception.ErrorCode;
import org.example.exception.GrouException;
import org.example.global.CampaignRedisCacheManager;
import org.example.service.KeywordService;
import org.example.service.LazySegmentTreeService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
        CompletableFuture.supplyAsync(() -> {
            if (lazySegmentTreeService.treeIsBuilding(email, year)) {
                log.info("Tree is Already Building! email: {}, year: {}", email, year);
                return null;
            }
            AllCampaignTypeData[] savedData = campaignRedisCacheManager.getCachedRawData(email, year);
            if (savedData != null) {
                log.debug("Redis 캐시 히트! DB 조회 생략.");
                return savedData;
            }
            log.debug("Redis 캐시 미스! DB 원본 조회 시작.");
            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = LocalDate.of(year, 12, 31);
            AllCampaignTypeData[] dbResult = keywordService
                    .getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByDate(startDate, endDate, email);
            campaignRedisCacheManager.saveRawData(email, year, dbResult);
            return dbResult;
            }, CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS, ioExecutor))
            .thenAcceptAsync(data -> {
                if (data == null) return;
                try {
                    log.info("메모리 세그먼트 트리 빌드 시작...");
                    lazySegmentTreeService.buildTree(email, year, data);
                    log.info("트리 빌드 완료!");
                } catch (Exception e) {
                    log.error("트리 빌드 중 에러 발생", e);
                    throw new GrouException(ErrorCode.UNKNOWN_ERROR);
                } finally {
                    lazySegmentTreeService.removeTreeBuildingKey(email, year);
                }
            }, cpuExecutor)
            .exceptionally(e -> {
                log.error("비동기 트리 빌드 파이프라인 전체에서 치명적 에러 발생!", e);
                lazySegmentTreeService.removeTreeBuildingKey(email, year);
                return null;
            });
    }
}
