package org.example.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AllCampaignTypeData;
import org.example.exception.ErrorCode;
import org.example.exception.GrouException;
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
    private final Executor ioExecutor;  // ioExecutor 빈도 주입받아야 함!
    private final Executor cpuExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTreeBuildEvent(TreeBuildEvent event) {
        CompletableFuture.runAsync(() -> {
            // Event가 Thread Safe 한지 체크!
            String email = event.getEmail();
            int year = event.getYear();
            if (lazySegmentTreeService.treeIsBuilding(email, year)) {
                log.info("Tree is Already Building! email: {}, year: {}", email, year);
                return;
            }
            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = LocalDate.of(year, 12, 31);
            Map<Integer, AllCampaignTypeData> dbResult= keywordService.
                    getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByDate(startDate, endDate,email);
            CompletableFuture.runAsync(() -> {
                try {
                  lazySegmentTreeService.buildTree(email,year,dbResult);
                } catch (Exception e) {
                    log.error("트리 빌드 중 에러 발생", e);
                    throw new GrouException(ErrorCode.UNKNOWN_ERROR);
                } finally {
                    lazySegmentTreeService.removeTreeBuildingKey(email,year); // 키 제거 확실히!
                }
            }, cpuExecutor);
        }, CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS, ioExecutor));
    }
}
