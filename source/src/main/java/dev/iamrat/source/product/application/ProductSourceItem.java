package dev.iamrat.source.product.application;

import dev.iamrat.source.support.error.SourceExceptionMessages;

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

    public ProductSourceItem {
        if (externalProductId == null || externalProductId.isBlank()) {
            throw new IllegalArgumentException(SourceExceptionMessages.EXTERNAL_PRODUCT_ID_MUST_NOT_BE_BLANK);
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(SourceExceptionMessages.PRODUCT_TITLE_MUST_NOT_BE_BLANK);
        }
        if (price == null) {
            throw new IllegalArgumentException(SourceExceptionMessages.PRODUCT_PRICE_MUST_NOT_BE_NULL);
        }
        externalProductId = externalProductId.trim();
        title = title.trim();
    }
}
