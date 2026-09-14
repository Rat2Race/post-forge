package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.util.List;

public record LaunchNewsPublishCommand(
    String keyword,
    Integer displayCount,
    Integer dailyCap,
    List<String> topics,
    BoardCategory category,
    PostPublishOrigin publishOrigin
) {
    public LaunchNewsPublishCommand {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("출시 뉴스 키워드는 비어 있을 수 없습니다");
        }
        keyword = keyword.trim();
        displayCount = displayCount == null ? 10 : Math.clamp(displayCount, 1, 100);
        dailyCap = dailyCap == null ? 3 : Math.clamp(dailyCap, 1, 20);
        category = category == null ? BoardCategory.GENERAL : category;
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
