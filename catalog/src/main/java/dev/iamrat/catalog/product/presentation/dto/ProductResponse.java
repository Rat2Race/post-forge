package dev.iamrat.catalog.product.presentation.dto;

import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.catalog.product.domain.ProductStatus;
import java.time.LocalDateTime;

public record ProductResponse(
    Long id,
    String source,
    String externalProductId,
    String name,
    String normalizedName,
    String brand,
    String maker,
    Long categoryId,
    String categoryName,
    String category1,
    String category2,
    String category3,
    Long currentPrice,
    String imageUrl,
    String productUrl,
    String mallName,
    ProductStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getSource(),
            product.getExternalProductId(),
            product.getName(),
            product.getNormalizedName(),
            product.getBrand(),
            product.getMaker(),
            product.getCategory() == null ? null : product.getCategory().getId(),
            product.getCategory() == null ? null : product.getCategory().getName(),
            product.getCategory1(),
            product.getCategory2(),
            product.getCategory3(),
            product.getCurrentPrice(),
            product.getImageUrl(),
            product.getProductUrl(),
            product.getMallName(),
            product.getStatus(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
