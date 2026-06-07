package dev.iamrat.catalog.matching.application;

import dev.iamrat.catalog.matching.domain.ProductEmbeddingVector;
import dev.iamrat.catalog.matching.domain.ProductMatchCandidateDraft;
import dev.iamrat.catalog.product.domain.Product;
import java.util.List;
import java.util.Optional;

public record ProductMatchDecision(
    Optional<Product> autoMatchedProduct,
    List<ProductMatchCandidateDraft> pendingCandidates,
    ProductEmbeddingVector embedding,
    String embeddingInput
) {

    public ProductMatchDecision {
        autoMatchedProduct = autoMatchedProduct == null ? Optional.empty() : autoMatchedProduct;
        pendingCandidates = pendingCandidates == null ? List.of() : List.copyOf(pendingCandidates);
    }

    public static ProductMatchDecision empty() {
        return new ProductMatchDecision(Optional.empty(), List.of(), null, null);
    }

    public static ProductMatchDecision withEmbedding(ProductEmbeddingVector embedding, String embeddingInput) {
        return new ProductMatchDecision(Optional.empty(), List.of(), embedding, embeddingInput);
    }
}
