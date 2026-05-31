package dev.iamrat.price.tracking.application;

import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.catalog.product.domain.Offer;
import dev.iamrat.core.event.DomainEventRecorder;
import dev.iamrat.core.event.EventType;
import dev.iamrat.price.tracking.domain.LowestPriceSnapshot;
import dev.iamrat.price.tracking.domain.PriceDropPolicy;
import dev.iamrat.price.tracking.domain.PriceSnapshot;
import dev.iamrat.price.tracking.infrastructure.persistence.LowestPriceSnapshotRepository;
import dev.iamrat.price.tracking.infrastructure.persistence.PriceSnapshotRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceSnapshotService {

    public static final String PRICE_SNAPSHOT_CREATED = "PriceSnapshotCreatedEvent";
    public static final String LOWEST_PRICE_CHANGED = "LowestPriceChangedEvent";
    public static final String PRICE_DROP_DETECTED = "PriceDropDetectedEvent";

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final LowestPriceSnapshotRepository lowestPriceSnapshotRepository;
    private final ObjectProvider<DomainEventRecorder> eventRecorderProvider;
    private final MeterRegistry meterRegistry;

    @Transactional
    public PriceSnapshot recordSnapshot(Product product, Offer offer, LocalDateTime collectedAt) {
        PriceSnapshot snapshot = priceSnapshotRepository.save(PriceSnapshot.create(
            product.getId(),
            offer.getId(),
            product.getSource(),
            product.getExternalProductId(),
            product.getCurrentPrice(),
            collectedAt == null ? LocalDateTime.now() : collectedAt
        ));
        counter("price_snapshot_created_total").increment();
        recordSnapshotCreated(snapshot);
        updateLowestPrice(snapshot);
        return snapshot;
    }

    public List<PriceSnapshot> getHistory(Long productId, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return priceSnapshotRepository.findByProductIdOrderByCollectedAtDesc(productId);
        }
        return priceSnapshotRepository.findByProductIdAndCollectedAtBetweenOrderByCollectedAtAsc(productId, from, to);
    }

    public List<LowestPriceSnapshot> getPriceDrops(int limit, PriceDropPolicy policy) {
        PriceDropPolicy effectivePolicy = policy == null ? PriceDropPolicy.defaultPolicy() : policy;
        return lowestPriceSnapshotRepository.findByDropRateGreaterThanEqualOrderByDropRateDesc(
            effectivePolicy.minDropRate(),
            PageRequest.of(0, Math.max(1, limit))
        );
    }

    private void updateLowestPrice(PriceSnapshot snapshot) {
        LowestPriceSnapshot lowest = lowestPriceSnapshotRepository.findByProductId(snapshot.getProductId())
            .orElseGet(() -> LowestPriceSnapshot.first(snapshot));
        boolean changed = lowest.updateIfChanged(snapshot);
        LowestPriceSnapshot saved = lowestPriceSnapshotRepository.save(lowest);
        if (changed) {
            counter("lowest_price_changed_total").increment();
            recordLowestPriceChanged(saved);
            if (saved.droppedAtLeast(PriceDropPolicy.defaultPolicy().minDropRate())) {
                counter("price_drop_detected_total").increment();
                recordPriceDropDetected(saved);
            }
        }
    }

    private void recordSnapshotCreated(PriceSnapshot snapshot) {
        record(PRICE_SNAPSHOT_CREATED, "Product", snapshot.getProductId().toString(), new PriceSnapshotCreatedPayload(
            snapshot.getProductId(),
            snapshot.getExternalProductId(),
            snapshot.getPrice(),
            snapshot.getCollectedAt()
        ));
    }

    private void recordLowestPriceChanged(LowestPriceSnapshot snapshot) {
        record(LOWEST_PRICE_CHANGED, "Product", snapshot.getProductId().toString(), new LowestPriceChangedPayload(
            snapshot.getProductId(),
            snapshot.getPreviousLowestPrice(),
            snapshot.getLowestPrice(),
            snapshot.getDropRate(),
            snapshot.getCollectedAt()
        ));
    }

    private void recordPriceDropDetected(LowestPriceSnapshot snapshot) {
        record(PRICE_DROP_DETECTED, "Product", snapshot.getProductId().toString(), new PriceDropDetectedPayload(
            snapshot.getProductId(),
            snapshot.getPreviousLowestPrice(),
            snapshot.getLowestPrice(),
            snapshot.getDropRate(),
            "PREVIOUS_LOWEST_PRICE_10_PERCENT",
            snapshot.getChangedAt()
        ));
    }

    private void record(String eventType, String aggregateType, String aggregateId, Object payload) {
        DomainEventRecorder recorder = eventRecorderProvider.getIfAvailable();
        if (recorder != null) {
            recorder.record(EventType.from(eventType), aggregateType, aggregateId, payload);
        }
    }

    private Counter counter(String name) {
        return Counter.builder(name).register(meterRegistry);
    }

    public record PriceSnapshotCreatedPayload(
        Long productId,
        String externalProductId,
        Long price,
        LocalDateTime collectedAt
    ) {
    }

    public record LowestPriceChangedPayload(
        Long productId,
        Long previousLowestPrice,
        Long newLowestPrice,
        java.math.BigDecimal dropRate,
        LocalDateTime collectedAt
    ) {
    }

    public record PriceDropDetectedPayload(
        Long productId,
        Long previousPrice,
        Long currentPrice,
        java.math.BigDecimal dropRate,
        String detectionRule,
        LocalDateTime detectedAt
    ) {
    }
}
