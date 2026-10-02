package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.NewsSection;
import dev.iamrat.source.news.application.NewsSourceItem;
import java.time.LocalDateTime;

public record LaunchNewsCandidate(
    String keyword,
    NewsSourceItem item,
    String canonicalUrl,
    String originalUrl,
    String sourceName,
    LocalDateTime publishedAt,
    NewsSection category
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
}
