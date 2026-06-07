package dev.iamrat.ingest.product.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.catalog.product.application.ProductService;
import dev.iamrat.catalog.product.application.ProductUpsertResult;
import dev.iamrat.catalog.product.domain.ProductUpsertCommand;
import dev.iamrat.ingest.product.domain.CollectionJob;
import dev.iamrat.ingest.product.domain.RawProduct;
import dev.iamrat.ingest.product.domain.TrackedKeyword;
import dev.iamrat.ingest.product.infrastructure.persistence.CollectionJobRepository;
import dev.iamrat.ingest.product.infrastructure.persistence.RawProductRepository;
import dev.iamrat.price.tracking.application.PriceSnapshotService;
import dev.iamrat.source.product.application.ProductSourceItem;
import dev.iamrat.source.product.application.ProductSourceQuery;
import dev.iamrat.source.product.application.ProductSourceResult;
import dev.iamrat.source.product.application.SourceRequestExecutor;
import dev.iamrat.source.product.domain.SourceType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectProductsUseCase {

    private final SourceRequestExecutor sourceRequestExecutor;
    private final ProductService productService;
    private final PriceSnapshotService priceSnapshotService;
    private final CollectionJobRepository collectionJobRepository;
    private final RawProductRepository rawProductRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Transactional
    public CollectionJob collect(TrackedKeyword trackedKeyword) {
        return collect(
            trackedKeyword.getId(),
            trackedKeyword.getSource(),
            trackedKeyword.getKeyword(),
            trackedKeyword.getDisplayCount()
        );
    }

    @Transactional
    public CollectionJob collect(SourceType source, String keyword, int displayCount) {
        return collect(null, source == null ? SourceType.MOCK : source, keyword, displayCount);
    }

    public Page<CollectionJob> getJobs(Pageable pageable) {
        return collectionJobRepository.findAllByOrderByRequestedAtDesc(pageable);
    }

    private CollectionJob collect(Long trackedKeywordId, SourceType source, String keyword, int displayCount) {
        CollectionJob job = collectionJobRepository.save(CollectionJob.requested(trackedKeywordId, source, keyword));
        Timer.Sample sample = Timer.start(meterRegistry);
        job.markRunning();

        try {
            ProductSourceResult result = sourceRequestExecutor.search(new ProductSourceQuery(source, keyword, displayCount));
            int count = 0;
            for (ProductSourceItem item : result.items()) {
                rawProductRepository.save(RawProduct.of(job, source, item.externalProductId(), serialize(item)));
                ProductUpsertResult upsertResult = productService.upsertWithOffer(toCommand(source, item));
                priceSnapshotService.recordSnapshot(upsertResult.product(), upsertResult.offer(), LocalDateTime.now());
                count++;
            }
            job.markSuccess(count);
            counter("collection_jobs_success_total").increment();
            Counter.builder("raw_products_saved_total").register(meterRegistry).increment(count);
        } catch (RuntimeException e) {
            job.markFailed(e.getMessage());
            counter("collection_jobs_failed_total").increment();
        } finally {
            sample.stop(Timer.builder("collection_jobs_duration_seconds").register(meterRegistry));
        }

        return job;
    }

    private ProductUpsertCommand toCommand(SourceType source, ProductSourceItem item) {
        return new ProductUpsertCommand(
            source.name(),
            item.externalProductId(),
            item.title(),
            item.brand(),
            item.maker(),
            item.category1(),
            item.category2(),
            item.category3(),
            item.price(),
            item.imageUrl(),
            item.productUrl(),
            item.mallName()
        );
    }

    private String serialize(ProductSourceItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("raw product payload must be JSON serializable", e);
        }
    }

    private Counter counter(String name) {
        return Counter.builder(name).register(meterRegistry);
    }
}
