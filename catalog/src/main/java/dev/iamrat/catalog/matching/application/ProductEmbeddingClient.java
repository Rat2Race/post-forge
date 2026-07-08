package dev.iamrat.catalog.matching.application;

import dev.iamrat.catalog.matching.domain.ProductEmbeddingVector;

public interface ProductEmbeddingClient {
    ProductEmbeddingVector embed(String input);
}
