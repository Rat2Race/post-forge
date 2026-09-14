package dev.iamrat.ingest.news.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductNewsIngestRequest(
    @NotBlank String keyword,
    @Min(1) @Max(100) Integer displayCount,
    @Size(max = 10) List<@Size(max = 30) String> topics
) {
}
