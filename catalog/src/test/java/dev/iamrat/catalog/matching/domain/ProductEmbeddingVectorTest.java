package dev.iamrat.catalog.matching.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductEmbeddingVectorTest {

    @Test
    @DisplayName("빈 임베딩 벡터 값은 거절한다")
    void rejectsEmptyValues() {
        assertThatThrownBy(() -> new ProductEmbeddingVector(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("상품 임베딩 벡터는 비어 있을 수 없습니다");

        assertThatThrownBy(() -> ProductEmbeddingVector.from(new float[0]))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("상품 임베딩 벡터는 비어 있을 수 없습니다");
    }
}
