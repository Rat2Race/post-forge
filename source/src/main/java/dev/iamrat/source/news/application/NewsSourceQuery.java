package dev.iamrat.source.news.application;

import dev.iamrat.source.support.error.SourceExceptionMessages;

public record NewsSourceQuery(
    String keyword,
    int displayCount,
    String sort
) {

    public NewsSourceQuery {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException(SourceExceptionMessages.NEWS_KEYWORD_MUST_NOT_BE_BLANK);
        }
        keyword = keyword.trim();
        displayCount = displayCount <= 0 ? 10 : Math.min(displayCount, 100);
        sort = sort == null || sort.isBlank() ? "date" : sort.trim();
        if (!sort.equals("date") && !sort.equals("sim")) {
            throw new IllegalArgumentException(SourceExceptionMessages.NEWS_SORT_MUST_BE_SUPPORTED);
        }
    }
}
