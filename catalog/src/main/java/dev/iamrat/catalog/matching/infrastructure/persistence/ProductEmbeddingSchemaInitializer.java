package dev.iamrat.catalog.matching.infrastructure.persistence;

import dev.iamrat.catalog.matching.application.ProductMatchingProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductEmbeddingSchemaInitializer {

    private static final int EMBEDDING_DIMENSIONS = 1536;

    private final JdbcTemplate jdbcTemplate;
    private final ProductMatchingProperties properties;

    @PostConstruct
    void initialize() {
        if (!properties.isEnabled()) {
            return;
        }
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS product_embeddings (
                product_id BIGINT PRIMARY KEY,
                embedding_input TEXT NOT NULL,
                embedding vector(%d) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
            """.formatted(EMBEDDING_DIMENSIONS));
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_product_embeddings_embedding
            ON product_embeddings USING hnsw (embedding vector_cosine_ops)
            """);
    }
}
