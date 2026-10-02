package dev.iamrat.ingest.news.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record LaunchNewsPublishResult(
    String keyword,
    List<Long> createdPostIds,
    List<Skip> skips
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

    /** 출시 뉴스는 기사 URL 단위로 건너뛴다. */
    public record Skip(String url, SkipReason reason) {
    }

    public enum SkipReason {
        DUPLICATE_ARTICLE,
        ADVERTISING,
        UNKNOWN_SOURCE,
        MISSING_LAUNCH_KEYWORD,
        AI_GENERATION_FAILED,
        DAILY_CAP_EXCEEDED
    }
}
