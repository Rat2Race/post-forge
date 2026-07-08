package dev.iamrat.ingest.news.application;

import java.util.List;

public record LaunchNewsAutoPostResult(
    String keyword,
    int acceptedCount,
    int skippedCount,
    List<Long> createdPostIds,
    List<LaunchNewsSkip> skips
) {
    public LaunchNewsAutoPostResult {
        createdPostIds = createdPostIds == null ? List.of() : List.copyOf(createdPostIds);
        skips = skips == null ? List.of() : List.copyOf(skips);
    }
}
