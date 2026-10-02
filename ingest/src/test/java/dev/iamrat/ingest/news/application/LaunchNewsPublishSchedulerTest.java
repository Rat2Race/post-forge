package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LaunchNewsPublishSchedulerTest {

    private final PublishLaunchNewsUseCase publishLaunchNewsUseCase = mock(PublishLaunchNewsUseCase.class);

    @Test
    @DisplayName("설정된 섹션마다 주제 확장 없이 시스템 배치 게시를 실행한다")
    void publishesEachConfiguredSectionAsSystemBatch() {
        given(publishLaunchNewsUseCase.publish(any())).willReturn(new LaunchNewsPublishResult("TECHNOLOGY", List.of(1L), List.of()));
        LaunchNewsPublishScheduler scheduler = new LaunchNewsPublishScheduler(
            publishLaunchNewsUseCase, List.of(" technology ,BUSINESS", "TECHNOLOGY"), 7, 2);

        scheduler.publishSectionNews();

        ArgumentCaptor<LaunchNewsPublishCommand> captor = ArgumentCaptor.forClass(LaunchNewsPublishCommand.class);
        verify(publishLaunchNewsUseCase, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(LaunchNewsPublishCommand::keyword).containsExactly("TECHNOLOGY", "BUSINESS");
        LaunchNewsPublishCommand first = captor.getAllValues().getFirst();
        assertThat(first.displayCount()).isEqualTo(7);
        assertThat(first.dailyCap()).isEqualTo(2);
        assertThat(first.topics()).isEmpty();
        assertThat(first.category()).isEqualTo(BoardCategory.TECHNOLOGY);
        assertThat(first.publishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
    }

    @Test
    @DisplayName("한 섹션이 실패해도 다음 섹션을 계속 처리한다")
    void continuesAfterFailedSection() {
        given(publishLaunchNewsUseCase.publish(any()))
            .willThrow(new IllegalStateException("피드 호출 실패"))
            .willReturn(new LaunchNewsPublishResult("BUSINESS", List.of(), List.of()));
        LaunchNewsPublishScheduler scheduler = new LaunchNewsPublishScheduler(
            publishLaunchNewsUseCase, List.of("TECHNOLOGY", "BUSINESS"), 5, 3);

        scheduler.publishSectionNews();

        verify(publishLaunchNewsUseCase, times(2)).publish(any());
    }

    @Test
    @DisplayName("섹션이 없거나 BoardCategory에 없는 이름이거나 범위를 벗어난 값이면 생성 자체를 거부한다")
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new LaunchNewsPublishScheduler(publishLaunchNewsUseCase, List.of("KERNEL"), 5, 3))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LaunchNewsPublishScheduler(publishLaunchNewsUseCase, List.of(" "), 5, 3))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LaunchNewsPublishScheduler(publishLaunchNewsUseCase, List.of("TECHNOLOGY"), 0, 3))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LaunchNewsPublishScheduler(publishLaunchNewsUseCase, List.of("TECHNOLOGY"), 5, 21))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
