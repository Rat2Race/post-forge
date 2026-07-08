package dev.iamrat.ingest.news.application;

public enum LaunchNewsSkipReason {
    DUPLICATE_ARTICLE,
    ADVERTISING,
    UNKNOWN_SOURCE,
    MISSING_LAUNCH_KEYWORD,
    AI_GENERATION_FAILED,
    DAILY_CAP_EXCEEDED
}
