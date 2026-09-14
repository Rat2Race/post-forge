package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.source.news.application.NewsSourceItem;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LaunchNewsCandidate(
    String keyword,
    NewsSourceItem item,
    String canonicalUrl,
    String originalUrl,
    String sourceName,
    LocalDateTime publishedAt,
    BoardCategory category
) {
    public String title() {
        return item.title();
    }

    public String description() {
        return item.description();
    }

    public String publishedAtText() {
        return item.publishedAt();
    }

    public LocalDate publishedDate(LocalDate fallback) {
        return publishedAt == null ? fallback : publishedAt.toLocalDate();
    }
}
