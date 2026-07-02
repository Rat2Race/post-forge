package dev.iamrat.ingest.news.presentation.dto;

import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.ingest.news.application.LaunchNewsAutoPostRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record LaunchNewsManualPostRequest(
    @NotBlank String keyword,
    Long productId,
    @Min(1) @Max(20) Integer displayCount,
    @Min(1) @Max(20) Integer dailyCap,
    @Size(max = 10) List<@Size(max = 30) String> topics
) {
    public LaunchNewsAutoPostRequest toCommand() {
        return new LaunchNewsAutoPostRequest(
            keyword,
            productId,
            displayCount,
            dailyCap,
            topics,
            PostPublishOrigin.ADMIN_BACKFILL
        );
    }
}
