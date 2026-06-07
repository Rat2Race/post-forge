package dev.iamrat.catalog.product.presentation.dto;

import dev.iamrat.catalog.product.domain.ProductUpsertCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductUpsertRequest(
    @NotBlank String source,
    @NotBlank String externalProductId,
    @NotBlank String name,
    String brand,
    String maker,
    String category1,
    String category2,
    String category3,
    @NotNull @PositiveOrZero Long currentPrice,
    String imageUrl,
    String productUrl,
    String mallName
) {
    public ProductUpsertCommand toCommand() {
        return new ProductUpsertCommand(
            source,
            externalProductId,
            name,
            brand,
            maker,
            category1,
            category2,
            category3,
            currentPrice,
            imageUrl,
            productUrl,
            mallName
        );
    }
}
