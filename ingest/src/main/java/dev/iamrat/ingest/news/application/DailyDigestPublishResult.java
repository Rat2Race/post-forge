package dev.iamrat.ingest.news.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.iamrat.core.board.post.BoardCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DailyDigestPublishResult(
    LocalDate newsDate,
    List<Long> createdPostIds,
    Map<BoardCategory, DailyDigestSkipReason> skips
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
}
