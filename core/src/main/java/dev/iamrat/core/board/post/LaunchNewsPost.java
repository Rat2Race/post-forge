package dev.iamrat.core.board.post;

import java.time.LocalDateTime;

public record LaunchNewsPost(
    LaunchNewsPostDraft draft,
    NewsSection section,
    PostPublishOrigin publishOrigin,
    String keyword,
    String sourceTitle,
    String canonicalUrl,
    String originalUrl,
    String sourceName,
    LocalDateTime publishedAt
) {
}
