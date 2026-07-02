package dev.iamrat.ingest.product.application;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingest.product.scheduler.enabled", havingValue = "true")
public class ProductCollectionScheduler {

    private final TrackedKeywordService trackedKeywordService;
    private final CollectProductsUseCase collectProductsUseCase;

    @Scheduled(cron = "${ingest.product.collection.cron:0 0 * * * *}")
    public void collectTrackedKeywords() {
        trackedKeywordService.findActive().forEach(collectProductsUseCase::collect);
    }
}
