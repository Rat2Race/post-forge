package dev.iamrat.catalog.product.presentation;

import dev.iamrat.catalog.product.domain.ProductCategory;

public record ProductCategoryResponse(
    Long id,
    String name,
    Long parentId,
    Integer depth
) {
    public static ProductCategoryResponse from(ProductCategory category) {
        return new ProductCategoryResponse(
            category.getId(),
            category.getName(),
            category.getParentId(),
            category.getDepth()
        );
    }
}
