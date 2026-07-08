package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.ingest.support.error.IngestExceptionMessages;
import java.util.List;

public record LaunchNewsAutoPostRequest(
    String keyword,
    Long productId,
    Integer displayCount,
    Integer dailyCap,
    List<String> topics,
    PostPublishOrigin publishOrigin
) {
    public LaunchNewsAutoPostRequest {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException(
                IngestExceptionMessages.LAUNCH_NEWS_KEYWORD_MUST_NOT_BE_BLANK
            );
        }
        keyword = keyword.trim();
        displayCount = displayCount == null ? 10 : Math.max(1, Math.min(displayCount, 20));
        dailyCap = dailyCap == null ? 3 : Math.max(1, Math.min(dailyCap, 20));
        topics = topics == null ? List.of() : topics.stream()
            .filter(topic -> topic != null && !topic.isBlank())
            .map(String::trim)
            .distinct()
            .limit(10)
            .toList();
        publishOrigin = publishOrigin == PostPublishOrigin.ADMIN_BACKFILL
            ? PostPublishOrigin.ADMIN_BACKFILL
            : PostPublishOrigin.SYSTEM_BATCH;
    }
}
