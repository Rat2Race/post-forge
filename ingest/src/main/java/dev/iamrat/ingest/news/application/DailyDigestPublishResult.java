package dev.iamrat.ingest.news.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.iamrat.core.board.post.NewsSection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DailyDigestPublishResult(
    LocalDate newsDate,
    List<Long> createdPostIds,
    Map<NewsSection, SkipReason> skips
) {
    public DailyDigestPublishResult {
        createdPostIds = createdPostIds == null ? List.of() : List.copyOf(createdPostIds);
        skips = skips == null ? Map.of() : Map.copyOf(skips);
    }

    @JsonProperty
    public int publishedCount() {
        return createdPostIds.size();
    }

    @JsonProperty
    public int skippedCount() {
        return skips.size();
    }

    /** 데일리는 분야 단위로 건너뛴다. */
    public enum SkipReason {
        NO_SOURCE,
        ALREADY_PUBLISHED,
        AI_GENERATION_FAILED
    }
}
