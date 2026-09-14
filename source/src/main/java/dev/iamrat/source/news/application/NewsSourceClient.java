package dev.iamrat.source.news.application;

import java.util.List;

public interface NewsSourceClient {
    List<NewsSourceItem> search(NewsSourceQuery query);
}
