package dev.iamrat.ai.search.infrastructure.vector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PgVectorPropertiesTest {

    @Test
    @DisplayName("PgVector 기본 차원과 스키마 초기화 설정을 유지한다")
    void pgVectorProperties_defaults() {
        PgVectorProperties properties = new PgVectorProperties();

        assertThat(properties.getDimensions()).isEqualTo(1536);
        assertThat(properties.isInitializeSchema()).isTrue();
    }
}
