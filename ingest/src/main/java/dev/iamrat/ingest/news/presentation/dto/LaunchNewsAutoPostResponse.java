package dev.iamrat.ingest.news.presentation.dto;

import dev.iamrat.ingest.news.application.LaunchNewsAutoPostResult;
import dev.iamrat.ingest.news.application.LaunchNewsSkip;
import dev.iamrat.ingest.news.application.LaunchNewsSkipReason;
import java.util.List;

public record LaunchNewsAutoPostResponse(
    String keyword,
    int acceptedCount,
    int skippedCount,
    List<Long> createdPostIds,
    List<SkipResponse> skips
) {
    public static LaunchNewsAutoPostResponse from(LaunchNewsAutoPostResult result) {
        return new LaunchNewsAutoPostResponse(
            result.keyword(),
            result.acceptedCount(),
            result.skippedCount(),
            result.createdPostIds(),
            result.skips().stream()
                .map(SkipResponse::from)
                .toList()
        );
    }

    public record SkipResponse(
        String url,
        LaunchNewsSkipReason reason
    ) {
        static SkipResponse from(LaunchNewsSkip skip) {
            return new SkipResponse(skip.url(), skip.reason());
        }
    }
}
