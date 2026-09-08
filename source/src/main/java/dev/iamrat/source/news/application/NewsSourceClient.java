package dev.iamrat.source.news.application;

public interface NewsSourceClient {
    NewsSourceResult search(NewsSourceQuery query);
}
