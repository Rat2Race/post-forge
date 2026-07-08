package dev.iamrat.source.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iamrat.source.product.domain.SourceType;
import dev.iamrat.source.product.infrastructure.mock.MockProductSourceClient;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoutingSourceRequestExecutorTest {

    @Test
    @DisplayName("source type에 맞는 client로 요청을 라우팅한다")
    void routesToClientBySourceType() {
        RoutingSourceRequestExecutor executor = new RoutingSourceRequestExecutor(List.of(new MockProductSourceClient()));

        ProductSourceResult result = executor.search(new ProductSourceQuery(SourceType.MOCK, "키보드", 3));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().externalProductId()).startsWith("mock-");
        assertThat(result.items().getFirst().price()).isEqualTo(9900L);
    }

    @Test
    @DisplayName("지원하지 않는 source type은 거절한다")
    void rejectsUnsupportedSource() {
        RoutingSourceRequestExecutor executor = new RoutingSourceRequestExecutor(List.of());

        assertThatThrownBy(() -> executor.search(new ProductSourceQuery(SourceType.NAVER, "키보드", 3)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("지원하지 않는 상품 소스");
    }
}
