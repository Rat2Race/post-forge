package dev.iamrat.price.check.presentation.dto;

import dev.iamrat.price.check.application.PriceCheckBasis;

public record PriceCheckBasisResponse(
    String provider,
    int sampleSize,
    long medianPrice,
    long lowThreshold,
    long highThreshold
) {
    public static PriceCheckBasisResponse from(PriceCheckBasis basis) {
        return new PriceCheckBasisResponse(
            basis.provider(),
            basis.sampleSize(),
            basis.medianPrice(),
            basis.lowThreshold(),
            basis.highThreshold()
        );
    }
}
