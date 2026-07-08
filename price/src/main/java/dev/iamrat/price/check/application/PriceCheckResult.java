package dev.iamrat.price.check.application;

import java.util.List;

public record PriceCheckResult(
    PriceJudgement judgement,
    PriceCheckConfidence confidence,
    long candidateEffectivePrice,
    boolean shippingIncludedVerified,
    String message,
    PriceCheckBasis basis,
    List<PriceCheckSampleItem> items
) {
    public PriceCheckResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
