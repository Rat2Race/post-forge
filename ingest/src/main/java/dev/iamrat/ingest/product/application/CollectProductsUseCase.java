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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
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

    public Page<CollectionJob> getCollectionJobs(Pageable pageable) {
        return collectionJobRepository.findAllByOrderByRequestedAtDesc(pageable);
    }

    private CollectionJob collect(Long trackedKeywordId, SourceType source, String keyword, int displayCount) {
        SourceType effectiveSource = source == null ? SourceType.MOCK : source;
        CollectionJob job = collectionJobRepository.save(
            CollectionJob.pending(trackedKeywordId, effectiveSource, keyword)
        );
        Timer.Sample sample = Timer.start(meterRegistry);
        job.markRunning();

        try {
            ProductSourceQuery query = new ProductSourceQuery(effectiveSource, keyword, displayCount);
            ProductSourceResult result = searchSource(query);
            int count = persistProducts(job, effectiveSource, result.items());
            job.markSuccess(count);
            counter("collection_jobs_success_total").increment();
            Counter.builder("raw_products_saved_total").register(meterRegistry).increment(count);
        } catch (RuntimeException e) {
            job.markFailed(e.getMessage());
            counter("collection_jobs_failed_total").increment();
            log.warn(
                "상품 수집 실패. jobId={}, source={}, keyword={}",
                job.getId(),
                effectiveSource,
                keyword,
                e
            );
        } finally {
            sample.stop(Timer.builder("collection_jobs_duration_seconds").register(meterRegistry));
        }

        return job;
    }

    private ProductSourceResult searchSource(ProductSourceQuery query) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return sourceRequestExecutor.search(query);
        } finally {
            sample.stop(Timer.builder("external_source_fetch")
                .tag("resource", "product")
                .tag("source", query.source().name())
                .register(meterRegistry));
        }
    }

    private int persistProducts(CollectionJob job, SourceType source, List<ProductSourceItem> items) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            int count = 0;
            for (ProductSourceItem item : items) {
                rawProductRepository.save(RawProduct.of(job, source, item.externalProductId(), serialize(item)));
                ProductUpsertResult upsertResult = productService.upsertWithOffer(toCommand(source, item));
                priceSnapshotService.recordSnapshot(upsertResult.product(), upsertResult.offer(), LocalDateTime.now());
                count++;
            }
            return count;
        } finally {
            sample.stop(Timer.builder("external_source_db_persist")
                .tag("resource", "product")
                .tag("source", source.name())
                .register(meterRegistry));
        }
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
            throw new IllegalArgumentException(
                "원본 상품 페이로드는 JSON으로 직렬화할 수 있어야 합니다",
                e
            );
        }
    }

    private Counter counter(String name) {
        return Counter.builder(name).register(meterRegistry);
    }
}
