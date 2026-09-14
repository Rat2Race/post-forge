package dev.iamrat.ingest.news.application;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty
    public int publishedCount() {
        return createdPostIds.size();
    }

    @JsonProperty
    public int skippedCount() {
        return skips.size();
    }
}
