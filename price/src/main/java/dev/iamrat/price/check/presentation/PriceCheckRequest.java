package dev.iamrat.price.check.presentation;

import dev.iamrat.price.check.application.PriceCheckCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record PriceCheckRequest(
    @NotBlank String keyword,
    String candidateUrl,
    @PositiveOrZero Long basePrice,
    @PositiveOrZero Long shippingFee,
    @PositiveOrZero Long discountAmount,
    @PositiveOrZero Long finalPaidPrice
) {
    public PriceCheckCommand toCommand() {
        return new PriceCheckCommand(keyword, candidateUrl, basePrice, shippingFee, discountAmount, finalPaidPrice);
    }
}
