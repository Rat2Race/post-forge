package dev.iamrat.ingest.news.presentation;

import java.time.Clock;
import java.time.LocalDate;

public record DailyDigestPublishRequest(LocalDate newsDate) {

    public LocalDate newsDateOrYesterday(Clock clock) {
        return newsDate != null ? newsDate : LocalDate.now(clock).minusDays(1);
    }
}
