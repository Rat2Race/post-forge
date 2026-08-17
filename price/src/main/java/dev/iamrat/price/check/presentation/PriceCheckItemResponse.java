package dev.iamrat.price.check.presentation;

import dev.iamrat.price.check.application.PriceCheckSampleItem;

public record PriceCheckItemResponse(
    String title,
    String mallName,
    long price,
    String link
) {
    public static PriceCheckItemResponse from(PriceCheckSampleItem item) {
        return new PriceCheckItemResponse(item.title(), item.mallName(), item.price(), item.link());
    }
}
