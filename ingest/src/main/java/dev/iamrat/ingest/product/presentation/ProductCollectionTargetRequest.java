package dev.iamrat.ingest.product.presentation;

import dev.iamrat.source.product.domain.SourceType;
import jakarta.validation.constraints.NotBlank;

public record ProductCollectionTargetRequest(
    SourceType source,
    @NotBlank String keyword,
    Integer displayCount
) {
}
