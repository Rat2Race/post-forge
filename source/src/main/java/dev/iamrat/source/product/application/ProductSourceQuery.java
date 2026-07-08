package dev.iamrat.source.product.application;

import dev.iamrat.source.product.domain.SourceType;
import dev.iamrat.source.support.error.SourceExceptionMessages;

public record ProductSourceQuery(SourceType source, String keyword, int displayCount) {

    private static final int DEFAULT_DISPLAY_COUNT = 10;
    private static final int MAX_DISPLAY_COUNT = 100;

    public ProductSourceQuery {
        if (source == null) {
            source = SourceType.MOCK;
        }
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException(SourceExceptionMessages.PRODUCT_SOURCE_KEYWORD_MUST_NOT_BE_BLANK);
        }
        keyword = keyword.trim();
        if (displayCount <= 0) {
            displayCount = DEFAULT_DISPLAY_COUNT;
        }
        displayCount = Math.min(displayCount, MAX_DISPLAY_COUNT);
    }
}
