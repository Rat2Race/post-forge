package dev.iamrat.price.tracking.application;

import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.catalog.product.domain.Offer;
import dev.iamrat.core.event.DomainEventRecorder;
import dev.iamrat.core.event.EventType;
import dev.iamrat.price.tracking.domain.PriceSnapshot;
import dev.iamrat.price.tracking.infrastructure.persistence.PriceSnapshotRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceSnapshotService {

    public static final String PRICE_SNAPSHOT_CREATED = "PriceSnapshotCreatedEvent";

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final List<DomainEventRecorder> eventRecorders;
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
        return snapshot;
    }

    public List<PriceSnapshot> getHistory(Long productId, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return priceSnapshotRepository.findByProductIdOrderByCollectedAtDesc(productId);
        }
        return priceSnapshotRepository.findByProductIdAndCollectedAtBetweenOrderByCollectedAtAsc(productId, from, to);
    }

    private void recordSnapshotCreated(PriceSnapshot snapshot) {
        record(PRICE_SNAPSHOT_CREATED, "Product", snapshot.getProductId().toString(), new PriceSnapshotCreatedPayload(
            snapshot.getProductId(),
            snapshot.getExternalProductId(),
            snapshot.getPrice(),
            snapshot.getCollectedAt()
        ));
    }

    private void record(String eventType, String aggregateType, String aggregateId, Object payload) {
        for (DomainEventRecorder recorder : eventRecorders) {
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

}
