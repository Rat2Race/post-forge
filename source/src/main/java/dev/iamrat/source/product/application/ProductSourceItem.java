package dev.iamrat.source.product.application;

public record ProductSourceItem(
    String externalProductId,
    String title,
    String brand,
    String maker,
    String category1,
    String category2,
    String category3,
    Long price,
    String imageUrl,
    String productUrl,
    String mallName
) {
}
