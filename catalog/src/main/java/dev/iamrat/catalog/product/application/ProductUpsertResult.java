package dev.iamrat.catalog.product.application;

import dev.iamrat.catalog.product.domain.Offer;
import dev.iamrat.catalog.product.domain.Product;

public record ProductUpsertResult(
    Product product,
    Offer offer
) {
}
