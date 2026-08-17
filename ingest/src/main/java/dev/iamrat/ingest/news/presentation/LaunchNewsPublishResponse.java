package dev.iamrat.ingest.news.presentation;

import dev.iamrat.ingest.news.application.LaunchNewsPublishResult;
import dev.iamrat.ingest.news.application.LaunchNewsSkip;
import dev.iamrat.ingest.news.application.LaunchNewsSkipReason;
import java.util.List;

public record LaunchNewsPublishResponse(
    String keyword,
    int publishedCount,
    int skippedCount,
    List<Long> createdPostIds,
    List<SkipResponse> skips
) {
    public static LaunchNewsPublishResponse from(LaunchNewsPublishResult result) {
        return new LaunchNewsPublishResponse(
            result.keyword(),
            result.publishedCount(),
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
