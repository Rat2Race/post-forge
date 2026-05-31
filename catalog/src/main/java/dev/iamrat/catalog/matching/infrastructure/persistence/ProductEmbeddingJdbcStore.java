package dev.iamrat.catalog.matching.infrastructure.persistence;

import dev.iamrat.catalog.matching.application.ProductEmbeddingSearchResult;
import dev.iamrat.catalog.matching.application.ProductEmbeddingStore;
import dev.iamrat.catalog.matching.domain.ProductEmbeddingVector;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductEmbeddingJdbcStore implements ProductEmbeddingStore {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(Long productId, String embeddingInput, ProductEmbeddingVector embedding) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            """
            INSERT INTO product_embeddings(product_id, embedding_input, embedding, created_at, updated_at)
            VALUES (?, ?, CAST(? AS vector), ?, ?)
            ON CONFLICT (product_id) DO UPDATE SET
                embedding_input = EXCLUDED.embedding_input,
                embedding = EXCLUDED.embedding,
                updated_at = EXCLUDED.updated_at
            """,
            productId,
            embeddingInput,
            embedding.toPgVectorLiteral(),
            Timestamp.valueOf(now),
            Timestamp.valueOf(now)
        );
    }

    @Override
    public List<ProductEmbeddingSearchResult> searchSimilar(ProductEmbeddingVector embedding, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        String vectorLiteral = embedding.toPgVectorLiteral();
        return jdbcTemplate.query(
            """
            SELECT product_id,
                   CAST(1 - (embedding <=> CAST(? AS vector)) AS NUMERIC(6, 4)) AS similarity_score
            FROM product_embeddings
            ORDER BY embedding <=> CAST(? AS vector)
            LIMIT ?
            """,
            (rs, rowNum) -> new ProductEmbeddingSearchResult(
                rs.getLong("product_id"),
                rs.getBigDecimal("similarity_score")
            ),
            vectorLiteral,
            vectorLiteral,
            limit
        );
    }
}
