package dev.iamrat.catalog.matching.infrastructure.persistence;

import dev.iamrat.catalog.matching.application.ProductMatchingProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProductEmbeddingSchemaInitializerTest {

    @Test
    @DisplayName("상품 매칭이 활성화되면 pgvector 상품 임베딩 스키마를 초기화한다")
    void initialize_whenEnabled_createsSchema() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProductMatchingProperties properties = new ProductMatchingProperties();

        new ProductEmbeddingSchemaInitializer(jdbcTemplate, properties).initialize();

        verify(jdbcTemplate).execute("CREATE EXTENSION IF NOT EXISTS vector");
        verify(jdbcTemplate).execute(contains("CREATE TABLE IF NOT EXISTS product_embeddings"));
        verify(jdbcTemplate).execute(contains("idx_product_embeddings_embedding"));
    }

    @Test
    @DisplayName("상품 매칭이 비활성화되면 스키마 초기화를 건너뛴다")
    void initialize_whenDisabled_skipsSchema() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProductMatchingProperties properties = new ProductMatchingProperties();
        properties.setEnabled(false);

        new ProductEmbeddingSchemaInitializer(jdbcTemplate, properties).initialize();

        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }
}
