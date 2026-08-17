package dev.iamrat.source.product.infrastructure.naver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iamrat.source.product.application.ProductSourceQuery;
import dev.iamrat.source.product.domain.SourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NaverShoppingSourceClientTest {

    @Test
    @DisplayName("종료된 네이버 쇼핑 API를 호출하지 않는다")
    void rejectsRetiredNaverShoppingApi() {
        NaverShoppingSourceClient client = new NaverShoppingSourceClient();

        assertThatThrownBy(() -> client.search(new ProductSourceQuery(SourceType.NAVER, "노트북", 5)))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("2026-07-31");
    }
}
