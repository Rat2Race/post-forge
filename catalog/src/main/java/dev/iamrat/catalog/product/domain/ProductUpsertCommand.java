package dev.iamrat.catalog.product.domain;

public record ProductUpsertCommand(
    String source,
    String externalProductId,
    String name,
    String brand,
    String maker,
    String category1,
    String category2,
    String category3,
    Long currentPrice,
    String imageUrl,
    String productUrl,
    String mallName
) {
    public String categoryName() {
        if (category3 != null && !category3.isBlank()) {
            return category3;
        }
        if (category2 != null && !category2.isBlank()) {
            return category2;
        }
        return category1;
    }
}
