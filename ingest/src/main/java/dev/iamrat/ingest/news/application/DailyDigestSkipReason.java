package dev.iamrat.ingest.news.application;

public enum DailyDigestSkipReason {
    NO_SOURCE,
    ALREADY_PUBLISHED,
    AI_GENERATION_FAILED
}
