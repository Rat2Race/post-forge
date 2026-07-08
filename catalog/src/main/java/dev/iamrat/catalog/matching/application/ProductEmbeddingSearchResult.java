package dev.iamrat.catalog.matching.application;

import java.math.BigDecimal;

public record ProductEmbeddingSearchResult(
    Long productId,
    BigDecimal similarityScore
) {
}
