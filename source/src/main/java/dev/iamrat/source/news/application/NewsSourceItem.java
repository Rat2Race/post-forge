package dev.iamrat.source.news.application;

import dev.iamrat.source.support.error.SourceExceptionMessages;

public record NewsSourceItem(
    String title,
    String description,
    String link,
    String originalLink,
    String publishedAt,
    String rawTitle,
    String rawDescription
) {

    public NewsSourceItem(
        String title,
        String description,
        String link,
        String originalLink,
        String publishedAt
    ) {
        this(title, description, link, originalLink, publishedAt, title, description);
    }

    public NewsSourceItem {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(SourceExceptionMessages.NEWS_TITLE_MUST_NOT_BE_BLANK);
        }
        if (link == null || link.isBlank()) {
            throw new IllegalArgumentException(SourceExceptionMessages.NEWS_LINK_MUST_NOT_BE_BLANK);
        }
        title = title.trim();
        link = link.trim();
        description = description == null ? "" : description.trim();
        originalLink = originalLink == null ? "" : originalLink.trim();
        publishedAt = publishedAt == null ? "" : publishedAt.trim();
        rawTitle = rawTitle == null ? "" : rawTitle.trim();
        rawDescription = rawDescription == null ? "" : rawDescription.trim();
    }
}
