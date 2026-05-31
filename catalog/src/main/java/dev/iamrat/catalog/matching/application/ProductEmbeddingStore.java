package dev.iamrat.catalog.matching.application;

import dev.iamrat.catalog.matching.domain.ProductEmbeddingVector;
import java.util.List;

public interface ProductEmbeddingStore {
    void save(Long productId, String embeddingInput, ProductEmbeddingVector embedding);

    List<ProductEmbeddingSearchResult> searchSimilar(ProductEmbeddingVector embedding, int limit);
}
