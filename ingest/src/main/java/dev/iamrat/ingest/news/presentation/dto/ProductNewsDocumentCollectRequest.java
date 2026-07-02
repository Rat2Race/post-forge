package dev.iamrat.ingest.news.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductNewsDocumentCollectRequest(
    Long productId,
    @NotBlank String keyword,
    Integer displayCount,
    @Size(max = 10) List<@Size(max = 30) String> topics
) {
}
