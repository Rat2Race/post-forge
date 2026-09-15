package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import dev.iamrat.ingest.news.infrastructure.persistence.TrackedKeywordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LaunchNewsPublishSchedulerConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withBean(TrackedKeywordRepository.class, () -> mock(TrackedKeywordRepository.class))
        .withBean(PublishLaunchNewsUseCase.class, () -> mock(PublishLaunchNewsUseCase.class))
        .withUserConfiguration(LaunchNewsPublishScheduler.class);

    @Test
    @DisplayName("두 플래그가 모두 true일 때만 배치 bean을 만든다")
    void registersSchedulerOnlyWhenBothFlagsAreTrue() {
        runner.withPropertyValues(
            "ingest.news.launch.scheduler.enabled=true",
            "source.naver-news.enabled=true"
        ).run(context -> assertThat(context).hasSingleBean(LaunchNewsPublishScheduler.class));
    }

    @Test
    @DisplayName("뉴스 수집이 꺼져 있으면 스케줄러 플래그가 켜져 있어도 배치 bean을 만들지 않는다")
    void doesNotRegisterSchedulerWhenNaverSourceIsDisabled() {
        runner.withPropertyValues(
            "ingest.news.launch.scheduler.enabled=true",
            "source.naver-news.enabled=false"
        ).run(context -> assertThat(context).doesNotHaveBean(LaunchNewsPublishScheduler.class));
    }

    @Test
    @DisplayName("스케줄러 플래그가 없으면 배치 bean을 만들지 않는다")
    void doesNotRegisterSchedulerWhenSchedulerFlagIsMissing() {
        runner.withPropertyValues("source.naver-news.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(LaunchNewsPublishScheduler.class));
    }
}
