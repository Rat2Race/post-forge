package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import dev.iamrat.core.board.post.NewsSection;
import dev.iamrat.ingest.news.application.DailyDigestPublishResult.SkipReason;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(MockitoExtension.class)
class DailyDigestSchedulerTest {

    @Mock
    private PublishDailyDigestUseCase publishDailyDigestUseCase;

    @Test
    @DisplayName("어제 뉴스 날짜로 데일리 브리핑 게시를 호출한다")
    void publishesYesterdayDigest() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-21T06:00:00Z"), ZoneId.of("Asia/Seoul"));
        given(publishDailyDigestUseCase.publish(LocalDate.of(2026, 8, 20)))
            .willReturn(new DailyDigestPublishResult(LocalDate.of(2026, 8, 20), List.of(), Map.of()));
        DailyDigestScheduler scheduler = new DailyDigestScheduler(publishDailyDigestUseCase, clock);

        scheduler.publishDailyDigest();

        verify(publishDailyDigestUseCase).publish(LocalDate.of(2026, 8, 20));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("게시 결과와 분야별 건너뛴 사유를 로그에 남긴다")
    void logsPublishedCountAndSkipReasons(CapturedOutput output) {
        LocalDate newsDate = LocalDate.of(2026, 8, 20);
        given(publishDailyDigestUseCase.publish(newsDate)).willReturn(new DailyDigestPublishResult(
            newsDate, List.of(7L), Map.of(NewsSection.HEALTH, SkipReason.NO_SOURCE)));
        Clock clock = Clock.fixed(Instant.parse("2026-08-21T06:00:00Z"), ZoneId.of("Asia/Seoul"));

        new DailyDigestScheduler(publishDailyDigestUseCase, clock).publishDailyDigest();

        assertThat(output).contains("newsDate=2026-08-20, published=1, skips={HEALTH=NO_SOURCE}");
    }
}
