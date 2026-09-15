package dev.iamrat.ingest.news.application;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingest.news.digest.scheduler.enabled", havingValue = "true")
public class DailyDigestScheduler {

    private final PublishDailyDigestUseCase publishDailyDigestUseCase;
    private final Clock clock;

    @Scheduled(cron = "${ingest.news.digest.cron:0 0 6 * * *}", zone = "Asia/Seoul")
    public void publishDailyDigest() {
        publishDailyDigestUseCase.publish(LocalDate.now(clock).minusDays(1));
    }
}
