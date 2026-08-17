package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.ingest.product.application.TrackedKeywordService;
import dev.iamrat.ingest.product.domain.TrackedKeyword;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingest.news.launch.scheduler.enabled", havingValue = "true")
public class LaunchNewsPublishScheduler {

    private final TrackedKeywordService trackedKeywordService;
    private final PublishLaunchNewsUseCase publishLaunchNewsUseCase;

    @Scheduled(cron = "${ingest.news.launch.cron:0 30 * * * *}")
    public void publishTrackedLaunchNews() {
        trackedKeywordService.findActive()
            .forEach(this::publishLaunchNews);
    }

    private void publishLaunchNews(TrackedKeyword trackedKeyword) {
        publishLaunchNewsUseCase.publish(new LaunchNewsPublishCommand(
            trackedKeyword.getKeyword(),
            null,
            trackedKeyword.getDisplayCount(),
            null,
            List.of(),
            PostPublishOrigin.SYSTEM_BATCH
        ));
    }
}
