package dev.iamrat.ingest.news.application;

import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.ingest.news.domain.TrackedKeyword;
import dev.iamrat.ingest.news.infrastructure.persistence.TrackedKeywordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"ingest.news.launch.scheduler.enabled", "source.naver-news.enabled"},
    havingValue = "true"
)
public class LaunchNewsPublishScheduler {

    private final TrackedKeywordRepository trackedKeywordRepository;
    private final PublishLaunchNewsUseCase publishLaunchNewsUseCase;

    @Scheduled(cron = "${ingest.news.launch.cron:0 30 * * * *}", zone = "Asia/Seoul")
    public void publishTrackedLaunchNews() {
        trackedKeywordRepository.findByEnabledTrue()
            .forEach(this::publishLaunchNews);
    }

    private void publishLaunchNews(TrackedKeyword trackedKeyword) {
        try {
            publishLaunchNewsUseCase.publish(new LaunchNewsPublishCommand(
                trackedKeyword.getKeyword(),
                trackedKeyword.getDisplayCount(),
                null,
                List.of(),
                trackedKeyword.getCategory(),
                PostPublishOrigin.SYSTEM_BATCH
            ));
        } catch (RuntimeException exception) {
            log.warn("출시 뉴스 게시 실패. keyword={}", trackedKeyword.getKeyword(), exception);
        }
    }
}
