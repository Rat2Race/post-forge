package dev.iamrat.ingest.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.catalog.product.application.ProductService;
import dev.iamrat.catalog.product.application.ProductUpsertResult;
import dev.iamrat.catalog.product.domain.Offer;
import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.ingest.product.domain.CollectionJob;
import dev.iamrat.ingest.product.domain.CollectionJobStatus;
import dev.iamrat.ingest.product.infrastructure.persistence.CollectionJobRepository;
import dev.iamrat.ingest.product.infrastructure.persistence.RawProductRepository;
import dev.iamrat.price.tracking.application.PriceSnapshotService;
import dev.iamrat.source.product.application.ProductSourceItem;
import dev.iamrat.source.product.application.ProductSourceQuery;
import dev.iamrat.source.product.application.ProductSourceResult;
import dev.iamrat.source.product.application.SourceRequestExecutor;
import dev.iamrat.source.product.domain.SourceType;
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
class CollectProductsUseCaseTest {

    @Mock
    private SourceRequestExecutor sourceRequestExecutor;

    @Mock
    private ProductService productService;

    @Mock
    private PriceSnapshotService priceSnapshotService;

    @Mock
    private CollectionJobRepository collectionJobRepository;

    @Mock
    private RawProductRepository rawProductRepository;

    @Test
    @DisplayName("수집한 원본 상품, 카탈로그 offer, 가격 snapshot을 저장한다")
    void collect_storesRawProductCatalogOfferAndPriceSnapshot() {
        ProductSourceItem item = new ProductSourceItem(
            "naver-123",
            "맥북 Air",
            "Apple",
            "Apple",
            "디지털/가전",
            "노트북",
            "맥북",
            1_234_000L,
            "https://img.example.com/123.png",
            "https://shopping.example.com/123",
            "네이버"
        );
        Product product = Product.builder()
            .source("NAVER")
            .externalProductId("naver-123")
            .name("맥북 Air")
            .normalizedName("맥북 air")
            .currentPrice(1_234_000L)
            .build();
        Offer offer = Offer.builder()
            .product(product)
            .source("NAVER")
            .externalProductId("naver-123")
            .title("맥북 Air")
            .build();
        given(collectionJobRepository.save(any(CollectionJob.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(sourceRequestExecutor.search(any(ProductSourceQuery.class)))
            .willReturn(new ProductSourceResult(List.of(item)));
        given(productService.upsertWithOffer(any())).willReturn(new ProductUpsertResult(product, offer));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        CollectProductsUseCase useCase = new CollectProductsUseCase(
            sourceRequestExecutor,
            productService,
            priceSnapshotService,
            collectionJobRepository,
            rawProductRepository,
            new ObjectMapper(),
            meterRegistry
        );

        CollectionJob job = useCase.collect(SourceType.NAVER, "맥북", 5);

        assertThat(job.getStatus()).isEqualTo(CollectionJobStatus.SUCCESS);
        assertThat(job.getCollectedCount()).isEqualTo(1);
        verify(sourceRequestExecutor).search(argThat(query ->
            query.source() == SourceType.NAVER
                && query.keyword().equals("맥북")
                && query.displayCount() == 5
        ));
        verify(rawProductRepository).save(argThat(rawProduct ->
            rawProduct.getSource() == SourceType.NAVER
                && rawProduct.getExternalProductId().equals("naver-123")
                && rawProduct.getRawPayload().contains("\"title\":\"맥북 Air\"")
        ));
        verify(productService).upsertWithOffer(argThat(command ->
            command.source().equals("NAVER")
                && command.externalProductId().equals("naver-123")
                && command.currentPrice().equals(1_234_000L)
                && command.mallName().equals("네이버")
        ));
        verify(priceSnapshotService).recordSnapshot(eq(product), eq(offer), any(LocalDateTime.class));
        assertThat(meterRegistry.find("external_source_fetch")
            .tag("resource", "product")
            .tag("source", "NAVER")
            .timer()
            .count()).isEqualTo(1);
        assertThat(meterRegistry.find("external_source_db_persist")
            .tag("resource", "product")
            .tag("source", "NAVER")
            .timer()
            .count()).isEqualTo(1);
    }
}
