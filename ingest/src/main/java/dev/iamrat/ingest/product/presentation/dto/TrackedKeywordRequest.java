package dev.iamrat.ingest.product.presentation.dto;

import dev.iamrat.source.product.domain.SourceType;
import jakarta.validation.constraints.NotBlank;

public record TrackedKeywordRequest(
    SourceType source,
    @NotBlank String keyword,
    Integer intervalMinutes,
    Integer displayCount
) {
}
