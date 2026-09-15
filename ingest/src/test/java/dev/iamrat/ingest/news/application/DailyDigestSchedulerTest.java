package dev.iamrat.ingest.news.application;

import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyDigestSchedulerTest {

    @Mock
    private PublishDailyDigestUseCase publishDailyDigestUseCase;

    @Test
    @DisplayName("어제 뉴스 날짜로 데일리 브리핑 게시를 호출한다")
    void publishesYesterdayDigest() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-21T06:00:00Z"), ZoneId.of("Asia/Seoul"));
        DailyDigestScheduler scheduler = new DailyDigestScheduler(publishDailyDigestUseCase, clock);

        scheduler.publishDailyDigest();

        verify(publishDailyDigestUseCase).publish(LocalDate.of(2026, 8, 20));
    }
}
