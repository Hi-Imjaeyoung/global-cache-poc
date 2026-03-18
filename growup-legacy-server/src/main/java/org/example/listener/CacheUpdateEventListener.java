package org.example.listener;

import lombok.RequiredArgsConstructor;
import org.example.service.LegacyLazySegmentTreeService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CacheUpdateEventListener {
    private final LegacyLazySegmentTreeService lazySegmentTreeService;
    private final Executor cpuExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCacheTreeUpdate(TreeUpdateEvent treeUpdateEvent){
        CompletableFuture.runAsync(()->{
            lazySegmentTreeService.updateTreeByPeriodData(treeUpdateEvent.getEmail(),treeUpdateEvent.getPreData());
        }, CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS, cpuExecutor));
    }
}
