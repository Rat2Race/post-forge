package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.DailyDigestDraft;
import dev.iamrat.core.board.post.DailyDigestDraftCommand;
import dev.iamrat.core.board.post.DailyDigestDraftGenerator;
import dev.iamrat.core.board.post.DailyDigestSourceItem;
import dev.iamrat.core.board.post.DailyDigestSourceReader;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostWriteCommand;
import dev.iamrat.core.board.post.PostWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublishDailyDigestUseCaseTest {

    private static final LocalDate NEWS_DATE = LocalDate.of(2026, 8, 20);
    private static final String DIGITAL_TITLE = "[디지털] 데일리 브리핑 - 2026-08-20";

    @Mock
    private DailyDigestSourceReader sourceReader;

    @Mock
    private DailyDigestDraftGenerator draftGenerator;

    @Mock
    private PostWriter postWriter;

    @InjectMocks
    private PublishDailyDigestUseCase useCase;

    @Test
    @DisplayName("출시 뉴스가 없는 분야는 NO_SOURCE로 건너뛴다")
    void publish_skipsCategoriesWithoutSource() {
        given(sourceReader.findLaunchNews(any(), any())).willReturn(List.of());

        DailyDigestPublishResult result = useCase.publish(NEWS_DATE);

        assertThat(result.createdPostIds()).isEmpty();
        assertThat(result.skips()).hasSize(BoardCategory.values().length);
        assertThat(result.skips())
            .allSatisfy((category, reason) -> assertThat(reason).isEqualTo(DailyDigestSkipReason.NO_SOURCE));
        verify(draftGenerator, never()).generate(any());
        verify(postWriter, never()).write(any());
    }

    @Test
    @DisplayName("이미 게시된 분야는 ALREADY_PUBLISHED로 건너뛰고 generator를 호출하지 않는다")
    void publish_skipsAlreadyPublishedCategoryWithoutCallingGenerator() {
        given(sourceReader.findLaunchNews(any(), any())).willReturn(List.of());
        given(sourceReader.findLaunchNews(BoardCategory.DIGITAL, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("갤럭시북 출시", "요약")));
        given(sourceReader.digestExists(BoardCategory.DIGITAL, DIGITAL_TITLE)).willReturn(true);

        DailyDigestPublishResult result = useCase.publish(NEWS_DATE);

        assertThat(result.skips()).containsEntry(BoardCategory.DIGITAL, DailyDigestSkipReason.ALREADY_PUBLISHED);
        assertThat(result.createdPostIds()).isEmpty();
        verify(draftGenerator, never()).generate(any());
        verify(postWriter, never()).write(any());
    }

    @Test
    @DisplayName("generator가 empty를 반환한 분야만 건너뛰고 다음 분야는 계속 진행한다")
    void publish_generatorFailureSkipsOnlyThatCategory() {
        given(sourceReader.findLaunchNews(any(), any())).willReturn(List.of());
        given(sourceReader.findLaunchNews(BoardCategory.DIGITAL, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("갤럭시북 출시", "요약")));
        given(sourceReader.findLaunchNews(BoardCategory.APPLIANCE, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("공기청정기 출시", "요약")));
        given(draftGenerator.generate(any())).willReturn(Optional.empty());
        given(draftGenerator.generate(argThatCategory(BoardCategory.APPLIANCE)))
            .willReturn(Optional.of(new DailyDigestDraft("가전 브리핑 본문", List.of("가전"))));
        given(postWriter.write(any())).willReturn(42L);

        DailyDigestPublishResult result = useCase.publish(NEWS_DATE);

        assertThat(result.skips()).containsEntry(BoardCategory.DIGITAL, DailyDigestSkipReason.AI_GENERATION_FAILED);
        assertThat(result.createdPostIds()).containsExactly(42L);
    }

    @Test
    @DisplayName("게시 커맨드에 데일리 브리핑 카테고리, 분야, 시스템 배치 출처, 제목 형식이 실린다")
    void publish_writesSystemBatchDailyDigestCommand() {
        given(sourceReader.findLaunchNews(any(), any())).willReturn(List.of());
        List<DailyDigestSourceItem> items = List.of(new DailyDigestSourceItem("갤럭시북 출시", "요약"));
        given(sourceReader.findLaunchNews(BoardCategory.DIGITAL, NEWS_DATE)).willReturn(items);
        given(draftGenerator.generate(any()))
            .willReturn(Optional.of(new DailyDigestDraft("디지털  브리핑\n본문", List.of("디지털", "출시"))));
        given(postWriter.write(any())).willReturn(7L);

        useCase.publish(NEWS_DATE);

        ArgumentCaptor<DailyDigestDraftCommand> draftCaptor = ArgumentCaptor.forClass(DailyDigestDraftCommand.class);
        verify(draftGenerator).generate(draftCaptor.capture());
        assertThat(draftCaptor.getValue()).isEqualTo(new DailyDigestDraftCommand(BoardCategory.DIGITAL, NEWS_DATE, items));

        ArgumentCaptor<PostWriteCommand> captor = ArgumentCaptor.forClass(PostWriteCommand.class);
        verify(postWriter).write(captor.capture());
        PostWriteCommand command = captor.getValue();
        assertThat(command.title()).isEqualTo(DIGITAL_TITLE);
        assertThat(command.content()).isEqualTo("디지털  브리핑\n본문");
        assertThat(command.summary()).isEqualTo("디지털 브리핑 본문");
        assertThat(command.tags()).containsExactly("디지털", "출시");
        assertThat(command.accountId()).isEqualTo(0L);
        assertThat(command.nickname()).isEqualTo("PostForge News Bot");
        assertThat(command.category()).isEqualTo(PostCategory.DAILY_DIGEST);
        assertThat(command.boardCategory()).isEqualTo(BoardCategory.DIGITAL);
        assertThat(command.publishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
    }

    @Test
    @DisplayName("본문이 500자를 넘으면 summary를 500자로 자른다")
    void publish_abbreviatesSummaryTo500Chars() {
        given(sourceReader.findLaunchNews(any(), any())).willReturn(List.of());
        given(sourceReader.findLaunchNews(BoardCategory.DIGITAL, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("갤럭시북 출시", "요약")));
        given(draftGenerator.generate(any()))
            .willReturn(Optional.of(new DailyDigestDraft("가".repeat(700), List.of())));
        given(postWriter.write(any())).willReturn(7L);

        useCase.publish(NEWS_DATE);

        ArgumentCaptor<PostWriteCommand> captor = ArgumentCaptor.forClass(PostWriteCommand.class);
        verify(postWriter).write(captor.capture());
        assertThat(captor.getValue().summary()).hasSize(500).isEqualTo("가".repeat(500));
    }

    @Test
    @DisplayName("summary를 자를 때 surrogate pair를 분리하지 않는다")
    void publish_doesNotSplitSurrogatePairInSummary() {
        given(sourceReader.findLaunchNews(any(), any())).willReturn(List.of());
        given(sourceReader.findLaunchNews(BoardCategory.DIGITAL, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("갤럭시북 출시", "요약")));
        given(draftGenerator.generate(any()))
            .willReturn(Optional.of(new DailyDigestDraft("가".repeat(499) + "😀나", List.of())));
        given(postWriter.write(any())).willReturn(7L);

        useCase.publish(NEWS_DATE);

        ArgumentCaptor<PostWriteCommand> captor = ArgumentCaptor.forClass(PostWriteCommand.class);
        verify(postWriter).write(captor.capture());
        assertThat(captor.getValue().summary()).isEqualTo("가".repeat(499));
    }

    @Test
    @DisplayName("한 분야 성공과 다른 분야 실패가 한 결과에 함께 담긴다")
    void publish_mixesSuccessAndSkipInOneResult() {
        given(sourceReader.findLaunchNews(any(), any())).willReturn(List.of());
        given(sourceReader.findLaunchNews(BoardCategory.DIGITAL, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("갤럭시북 출시", "요약")));
        given(sourceReader.findLaunchNews(BoardCategory.APPLIANCE, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("공기청정기 출시", "요약")));
        given(sourceReader.digestExists(eq(BoardCategory.APPLIANCE), any())).willReturn(true);
        given(draftGenerator.generate(any()))
            .willReturn(Optional.of(new DailyDigestDraft("디지털 브리핑 본문", List.of())));
        given(postWriter.write(any())).willReturn(11L);

        DailyDigestPublishResult result = useCase.publish(NEWS_DATE);

        assertThat(result.createdPostIds()).containsExactly(11L);
        assertThat(result.skips()).containsEntry(BoardCategory.APPLIANCE, DailyDigestSkipReason.ALREADY_PUBLISHED);
        assertThat(result.skips()).doesNotContainKey(BoardCategory.DIGITAL);
        assertThat(result.newsDate()).isEqualTo(NEWS_DATE);
    }

    private DailyDigestDraftCommand argThatCategory(BoardCategory category) {
        return org.mockito.ArgumentMatchers.argThat(command -> command != null && command.category() == category);
    }
}
