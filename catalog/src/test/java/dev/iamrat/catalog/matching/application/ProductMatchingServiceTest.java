package dev.iamrat.catalog.matching.application;

import dev.iamrat.catalog.matching.domain.ProductEmbeddingVector;
import dev.iamrat.catalog.matching.infrastructure.persistence.ProductMatchCandidateRepository;
import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.catalog.product.domain.ProductCategory;
import dev.iamrat.catalog.product.domain.ProductUpsertCommand;
import dev.iamrat.catalog.product.infrastructure.persistence.ProductRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductMatchingServiceTest {

    @Mock
    private ProductEmbeddingClient embeddingClient;

    @Mock
    private ProductEmbeddingStore embeddingStore;

    @Mock
    private ProductMatchCandidateRepository candidateRepository;

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품 매칭이 비활성화되면 임베딩 호출 없이 빈 결정을 반환한다")
    void match_whenDisabled_returnsEmptyDecision() {
        ProductMatchingProperties properties = new ProductMatchingProperties();
        properties.setEnabled(false);
        ProductMatchingService service = service(properties);

        ProductMatchDecision decision = service.match(command(), ProductCategory.root("노트북"));

        assertThat(decision.autoMatchedProduct()).isEmpty();
        assertThat(decision.pendingCandidates()).isEmpty();
        verify(embeddingClient, never()).embed(any());
    }

    @Test
    @DisplayName("선택 기능인 상품 매칭이 실패해도 상품 갱신 저장을 막지 않도록 빈 결정을 반환한다")
    void match_whenEmbeddingUnavailable_returnsEmptyDecision() {
        ProductMatchingService service = service(new ProductMatchingProperties());
        given(embeddingClient.embed(any())).willThrow(new IllegalStateException("quota"));

        ProductMatchDecision decision = service.match(command(), ProductCategory.root("노트북"));

        assertThat(decision.autoMatchedProduct()).isEmpty();
        assertThat(decision.pendingCandidates()).isEmpty();
        assertThat(decision.embedding()).isNull();
        assertThat(decision.embeddingInput()).isNull();
    }

    @Test
    @DisplayName("상품 임베딩 색인 저장 실패는 상품 갱신 저장 성공을 되돌리지 않는다")
    void indexProduct_whenEmbeddingStoreUnavailable_doesNotThrow() {
        ProductMatchingService service = service(new ProductMatchingProperties());
        ProductEmbeddingVector embedding = new ProductEmbeddingVector(List.of(0.1, 0.2, 0.3));
        ProductMatchDecision decision = ProductMatchDecision.withEmbedding(embedding, "embedding input");
        Product product = product();
        willThrow(new IllegalStateException("pgvector unavailable"))
            .given(embeddingStore)
            .save(eq(product.getId()), eq("embedding input"), eq(embedding));

        assertThatCode(() -> service.indexProduct(product, command(), decision))
            .doesNotThrowAnyException();
    }

    private ProductMatchingService service(ProductMatchingProperties properties) {
        return new ProductMatchingService(
            embeddingClient,
            embeddingStore,
            candidateRepository,
            productRepository,
            properties
        );
    }

    private ProductUpsertCommand command() {
        return new ProductUpsertCommand(
            "NAVER",
            "external-1",
            "테스트 노트북",
            "브랜드",
            "제조사",
            "디지털",
            "PC",
            "노트북",
            1_000_000L,
            "https://example.com/image.jpg",
            "https://example.com/product",
            "테스트몰"
        );
    }

    private Product product() {
        return Product.builder()
            .id(7L)
            .source("NAVER")
            .externalProductId("external-1")
            .name("테스트 노트북")
            .normalizedName(Product.normalizeName("테스트 노트북"))
            .currentPrice(1_000_000L)
            .build();
    }

}
