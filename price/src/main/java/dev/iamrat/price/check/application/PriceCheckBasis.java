package dev.iamrat.price.check.application;

public record PriceCheckBasis(
    String provider,
    int sampleSize,
    long medianPrice,
    long lowThreshold,
    long highThreshold
) {
}
