package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.ingest.product.application.TrackedKeywordService;
import dev.iamrat.ingest.product.domain.TrackedKeyword;
import dev.iamrat.source.product.domain.SourceType;
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
    private TrackedKeywordService trackedKeywordService;

    @Mock
    private PublishLaunchNewsUseCase publishLaunchNewsUseCase;

    @InjectMocks
    private LaunchNewsPublishScheduler scheduler;

    @Test
    @DisplayName("활성 추적 키워드를 시스템 배치 출시 뉴스로 게시한다")
    void publishesActiveTrackedKeywordsAsSystemBatchLaunchNews() {
        given(trackedKeywordService.findActive()).willReturn(List.of(
            TrackedKeyword.register(SourceType.NAVER, "갤럭시북", 7)
        ));

        scheduler.publishTrackedLaunchNews();

        ArgumentCaptor<LaunchNewsPublishCommand> captor = ArgumentCaptor.forClass(LaunchNewsPublishCommand.class);
        verify(publishLaunchNewsUseCase).publish(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("갤럭시북");
        assertThat(captor.getValue().displayCount()).isEqualTo(7);
        assertThat(captor.getValue().publishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
        assertThat(captor.getValue().productId()).isNull();
    }
}
