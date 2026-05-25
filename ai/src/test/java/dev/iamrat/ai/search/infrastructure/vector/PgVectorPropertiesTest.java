package dev.iamrat.ai.search.infrastructure.vector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;

class PgVectorPropertiesTest {

    @Test
    @DisplayName("spring.ai.vectorstore.pgvector prefix로 PgVector 설정을 바인딩한다")
    void pgVectorProperties_usesPgVectorPrefix() {
        ConfigurationProperties annotation =
            PgVectorProperties.class.getAnnotation(ConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("spring.ai.vectorstore.pgvector");
    }

    @Test
    @DisplayName("PgVector 기본 dimension과 schema 초기화 설정을 유지한다")
    void pgVectorProperties_defaults() {
        PgVectorProperties properties = new PgVectorProperties();

        assertThat(properties.getDimensions()).isEqualTo(1536);
        assertThat(properties.isInitializeSchema()).isTrue();
    }
}
