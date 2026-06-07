package dev.iamrat.source.product.application;

import dev.iamrat.source.product.domain.SourceType;

public record ProductSourceQuery(
    SourceType source,
    String keyword,
    int displayCount
) {
}
