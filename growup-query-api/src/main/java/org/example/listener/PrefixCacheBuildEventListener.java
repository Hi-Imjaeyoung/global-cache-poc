package org.example.listener;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.CampaignRedisCacheManager;
import org.example.dto.AllCampaignTypeData;
import org.example.service.KeywordService;
import org.example.service.PrefixSumService;
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
public class PrefixCacheBuildEventListener {

    private final PrefixSumService prefixSumService;
    private final CampaignRedisCacheManager campaignRedisCacheManager;
    private final KeywordService keywordService;
    private final Executor ioExecutor;  // ioExecutor 빈도 주입받아야 함!
    private final Executor cpuExecutor;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePrefixBuildEvent(PrefixBuildEvent event){
        String email = event.getEmail();
        int year = event.getYear();
        if (prefixSumService.prefixIsBuilding(email, year)) {
            log.info("Tree is Already Building! email: {}, year: {}", email, year);
            return;
        }
        CompletableFuture.supplyAsync(()->{
                    try{
                        return campaignRedisCacheManager.getCachedPrefixData(email, year);
                    }catch (Exception e){
                        log.warn("Redis L2 조회 실패 또는 서킷 Open 상태! DB 원본 조회로 이어갑니다.");
                        return null;
                    }
                },CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS,ioExecutor))
                .thenApplyAsync(data -> {
                    if(data != null){
                        log.debug("Redis 캐시 Hit");
                        prefixSumService.saveBackupPrefixData(email,year,data);
                        return null;
                    }else{
                        log.debug("Redis 캐시 미스! DB 원본 조회 시작.");
                        CircuitBreaker.State state = circuitBreakerRegistry.circuitBreaker("redisCircuitBreaker").getState();
                        if(state != CircuitBreaker.State.CLOSED){
                            log.warn("Redis 서킷 Open 상태. DB 조회를 철회합니다. {}:{} L1 데이터 삭제 실시합니다.",email,year);
                            prefixSumService.removeAllPrefixData();
                            return null;
                        }
                        LocalDate startDate = LocalDate.of(year, 1, 1);
                        LocalDate endDate = LocalDate.of(year, 12, 31);
                        return keywordService
                                .getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByDate(startDate, endDate, email);
                    }
                }, ioExecutor)
                .thenApplyAsync(data -> {
                    if (data == null) return null;
                    log.debug("메모리 세그먼트 트리 빌드 시작...");
                    return prefixSumService.startBuildPrefix(email, year, data);
                },cpuExecutor)
                .thenAcceptAsync(segTree ->{
                    if(segTree == null) return;
                    campaignRedisCacheManager.savePrefixData(email, year, segTree);
                    log.debug("Redis L2 캐시 저장 완료!");
                },ioExecutor)
                .whenComplete((result,ex)->{
                    prefixSumService.removeTreeBuildingKey(email, year);
                    if (ex != null) {
                        log.error("비동기 트리 빌드 파이프라인에서 치명적 에러 발생! [email: {}]", email, ex);
                    }
                });
    }
}
