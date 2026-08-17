package dev.iamrat.price.check.application;

public record PriceCheckCommand(
    String keyword,
    String candidateUrl,
    Long basePrice,
    Long shippingFee,
    Long discountAmount,
    Long finalPaidPrice
) {
}
