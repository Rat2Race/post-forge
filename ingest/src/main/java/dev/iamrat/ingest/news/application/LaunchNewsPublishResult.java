package dev.iamrat.ingest.news.application;

import java.util.List;

public record LaunchNewsPublishResult(
    String keyword,
    List<Long> createdPostIds,
    List<LaunchNewsSkip> skips
) {
    public LaunchNewsPublishResult {
        createdPostIds = createdPostIds == null ? List.of() : List.copyOf(createdPostIds);
        skips = skips == null ? List.of() : List.copyOf(skips);
    }

    public int publishedCount() {
        return createdPostIds.size();
    }

    public int skippedCount() {
        return skips.size();
    }
}
