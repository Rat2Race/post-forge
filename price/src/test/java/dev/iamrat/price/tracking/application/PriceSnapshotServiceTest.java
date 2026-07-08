package dev.iamrat.price.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import dev.iamrat.catalog.product.domain.Offer;
import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.core.event.DomainEventRecorder;
import dev.iamrat.core.event.EventType;
import dev.iamrat.price.tracking.domain.PriceSnapshot;
import dev.iamrat.price.tracking.infrastructure.persistence.PriceSnapshotRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceSnapshotServiceTest {

    @Mock
    private PriceSnapshotRepository priceSnapshotRepository;

    @Mock
    private DomainEventRecorder eventRecorder;

    @Test
    @DisplayName("높은 가격과 낮은 가격 snapshot을 추가 이벤트 없이 저장한다")
    void recordSnapshotStoresBothHigherAndLowerPricesWithoutExtraEvents() {
        LocalDateTime collectedAt = LocalDateTime.of(2026, 6, 8, 12, 0);
        given(priceSnapshotRepository.save(any(PriceSnapshot.class))).willAnswer(invocation -> invocation.getArgument(0));
        PriceSnapshotService service = new PriceSnapshotService(
            priceSnapshotRepository,
            List.of(eventRecorder),
            new SimpleMeterRegistry()
        );

        service.recordSnapshot(product(10_000L), offer(), collectedAt);
        service.recordSnapshot(product(12_000L), offer(), collectedAt.plusHours(1));
        service.recordSnapshot(product(8_000L), offer(), collectedAt);

        ArgumentCaptor<PriceSnapshot> snapshotCaptor = ArgumentCaptor.forClass(PriceSnapshot.class);
        verify(priceSnapshotRepository, times(3)).save(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getAllValues())
            .extracting(PriceSnapshot::getPrice)
            .containsExactly(10_000L, 12_000L, 8_000L);
        verify(eventRecorder, times(3)).record(
            eq(EventType.from(PriceSnapshotService.PRICE_SNAPSHOT_CREATED)),
            eq("Product"),
            eq("1"),
            any(PriceSnapshotService.PriceSnapshotCreatedPayload.class)
        );
        verifyNoMoreInteractions(eventRecorder);
    }

    private Product product(Long price) {
        return Product.builder()
            .id(1L)
            .source("NAVER")
            .externalProductId("naver-123")
            .name("맥북 Air")
            .normalizedName("맥북 air")
            .currentPrice(price)
            .build();
    }

    private Offer offer() {
        return Offer.builder()
            .id(11L)
            .source("NAVER")
            .externalProductId("naver-123")
            .title("맥북 Air")
            .build();
    }
}
