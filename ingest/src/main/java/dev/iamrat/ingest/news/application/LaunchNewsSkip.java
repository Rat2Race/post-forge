package dev.iamrat.ingest.news.application;

public record LaunchNewsSkip(
    String url,
    LaunchNewsSkipReason reason
) {
}
