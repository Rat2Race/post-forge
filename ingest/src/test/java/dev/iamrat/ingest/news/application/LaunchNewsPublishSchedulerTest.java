package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.ingest.news.domain.TrackedKeyword;
import dev.iamrat.ingest.news.infrastructure.persistence.TrackedKeywordRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LaunchNewsPublishSchedulerTest {

    @Mock
    private TrackedKeywordRepository trackedKeywordRepository;

    @Mock
    private PublishLaunchNewsUseCase publishLaunchNewsUseCase;

    @InjectMocks
    private LaunchNewsPublishScheduler scheduler;

    @Test
    @DisplayName("활성 추적 키워드를 시스템 배치 출시 뉴스로 게시한다")
    void publishesActiveTrackedKeywordsAsSystemBatchLaunchNews() {
        given(trackedKeywordRepository.findByEnabledTrue()).willReturn(List.of(
            TrackedKeyword.builder()
                .keyword("갤럭시북")
                .displayCount(7)
                .enabled(true)
                .category(BoardCategory.DIGITAL)
                .build()
        ));

        scheduler.publishTrackedLaunchNews();

        ArgumentCaptor<LaunchNewsPublishCommand> captor = ArgumentCaptor.forClass(LaunchNewsPublishCommand.class);
        verify(publishLaunchNewsUseCase).publish(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("갤럭시북");
        assertThat(captor.getValue().displayCount()).isEqualTo(7);
        assertThat(captor.getValue().category()).isEqualTo(BoardCategory.DIGITAL);
        assertThat(captor.getValue().publishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
    }

    @Test
    @DisplayName("키워드 하나가 실패해도 남은 키워드를 계속 게시한다")
    void keepsPublishingRemainingKeywordsWhenOneKeywordFails() {
        given(trackedKeywordRepository.findByEnabledTrue()).willReturn(List.of(
            trackedKeyword("실패키워드"),
            trackedKeyword("정상키워드")
        ));
        given(publishLaunchNewsUseCase.publish(any(LaunchNewsPublishCommand.class)))
            .willThrow(new IllegalStateException("Naver 호출 실패"))
            .willReturn(new LaunchNewsPublishResult("정상키워드", List.of(1L), List.of()));

        scheduler.publishTrackedLaunchNews();

        ArgumentCaptor<LaunchNewsPublishCommand> captor = ArgumentCaptor.forClass(LaunchNewsPublishCommand.class);
        verify(publishLaunchNewsUseCase, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(LaunchNewsPublishCommand::keyword)
            .containsExactly("실패키워드", "정상키워드");
    }

    private TrackedKeyword trackedKeyword(String keyword) {
        return TrackedKeyword.builder()
            .keyword(keyword)
            .displayCount(5)
            .enabled(true)
            .category(BoardCategory.DIGITAL)
            .build();
    }
}
