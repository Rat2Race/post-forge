package dev.iamrat.source.news.application;

import java.util.List;

public record NewsSourceResult(
    List<NewsSourceItem> items
) {

    public NewsSourceResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
