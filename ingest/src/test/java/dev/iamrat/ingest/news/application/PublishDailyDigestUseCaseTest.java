package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.board.post.DailyDigestDraft;
import dev.iamrat.core.board.post.DailyDigestDraftCommand;
import dev.iamrat.core.board.post.DailyDigestDraftGenerator;
import dev.iamrat.core.board.post.DailyDigestSourceItem;
import dev.iamrat.core.board.post.NewsPostPort;
import dev.iamrat.core.board.post.NewsSection;
import dev.iamrat.ingest.news.application.DailyDigestPublishResult.SkipReason;
import java.time.LocalDate;
import java.util.List;
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

    @Mock
    private NewsPostPort newsPosts;

    @Mock
    private DailyDigestDraftGenerator draftGenerator;

    @InjectMocks
    private PublishDailyDigestUseCase useCase;

    @Test
    @DisplayName("출시 뉴스가 없는 분야는 NO_SOURCE로 건너뛴다")
    void publish_skipsSectionsWithoutSource() {
        given(newsPosts.findLaunchNews(any(), any())).willReturn(List.of());

        DailyDigestPublishResult result = useCase.publish(NEWS_DATE);

        assertThat(result.createdPostIds()).isEmpty();
        assertThat(result.skips()).hasSize(NewsSection.values().length);
        assertThat(result.skips())
            .allSatisfy((section, reason) -> assertThat(reason).isEqualTo(SkipReason.NO_SOURCE));
        verify(draftGenerator, never()).generate(any());
        verify(newsPosts, never()).publishDailyDigest(any(), any(), any());
    }

    @Test
    @DisplayName("이미 게시된 분야는 ALREADY_PUBLISHED로 건너뛰고 generator를 호출하지 않는다")
    void publish_skipsAlreadyPublishedSectionWithoutCallingGenerator() {
        given(newsPosts.findLaunchNews(any(), any())).willReturn(List.of());
        given(newsPosts.findLaunchNews(NewsSection.TECHNOLOGY, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("갤럭시북 출시", "요약")));
        given(newsPosts.digestExists(NewsSection.TECHNOLOGY, NEWS_DATE)).willReturn(true);

        DailyDigestPublishResult result = useCase.publish(NEWS_DATE);

        assertThat(result.skips()).containsEntry(NewsSection.TECHNOLOGY, SkipReason.ALREADY_PUBLISHED);
        assertThat(result.createdPostIds()).isEmpty();
        verify(draftGenerator, never()).generate(any());
        verify(newsPosts, never()).publishDailyDigest(any(), any(), any());
    }

    @Test
    @DisplayName("generator가 empty를 반환한 분야만 건너뛰고 다음 분야는 계속 진행한다")
    void publish_generatorFailureSkipsOnlyThatSection() {
        given(newsPosts.findLaunchNews(any(), any())).willReturn(List.of());
        given(newsPosts.findLaunchNews(NewsSection.TECHNOLOGY, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("갤럭시북 출시", "요약")));
        given(newsPosts.findLaunchNews(NewsSection.BUSINESS, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("공기청정기 출시", "요약")));
        given(draftGenerator.generate(any())).willReturn(Optional.empty());
        given(draftGenerator.generate(argThatSection(NewsSection.BUSINESS)))
            .willReturn(Optional.of(new DailyDigestDraft("가전 브리핑 본문", List.of("가전"))));
        given(newsPosts.publishDailyDigest(any(), any(), any())).willReturn(42L);

        DailyDigestPublishResult result = useCase.publish(NEWS_DATE);

        assertThat(result.skips()).containsEntry(NewsSection.TECHNOLOGY, SkipReason.AI_GENERATION_FAILED);
        assertThat(result.createdPostIds()).containsExactly(42L);
    }

    @Test
    @DisplayName("분야의 출시 뉴스로 초안을 만들어 그 분야·날짜의 데일리 브리핑으로 게시한다")
    void publish_draftsFromSectionNewsAndPublishesDigestForThatSectionAndDate() {
        given(newsPosts.findLaunchNews(any(), any())).willReturn(List.of());
        List<DailyDigestSourceItem> items = List.of(new DailyDigestSourceItem("갤럭시북 출시", "요약"));
        given(newsPosts.findLaunchNews(NewsSection.TECHNOLOGY, NEWS_DATE)).willReturn(items);
        DailyDigestDraft draft = new DailyDigestDraft("디지털 브리핑 본문", List.of("디지털", "출시"));
        given(draftGenerator.generate(any())).willReturn(Optional.of(draft));
        given(newsPosts.publishDailyDigest(any(), any(), any())).willReturn(7L);

        DailyDigestPublishResult result = useCase.publish(NEWS_DATE);

        ArgumentCaptor<DailyDigestDraftCommand> draftCaptor = ArgumentCaptor.forClass(DailyDigestDraftCommand.class);
        verify(draftGenerator).generate(draftCaptor.capture());
        assertThat(draftCaptor.getValue()).isEqualTo(new DailyDigestDraftCommand(NewsSection.TECHNOLOGY, NEWS_DATE, items));
        verify(newsPosts).publishDailyDigest(NewsSection.TECHNOLOGY, NEWS_DATE, draft);
        assertThat(result.createdPostIds()).containsExactly(7L);
    }

    @Test
    @DisplayName("한 분야 성공과 다른 분야 실패가 한 결과에 함께 담긴다")
    void publish_mixesSuccessAndSkipInOneResult() {
        given(newsPosts.findLaunchNews(any(), any())).willReturn(List.of());
        given(newsPosts.findLaunchNews(NewsSection.TECHNOLOGY, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("갤럭시북 출시", "요약")));
        given(newsPosts.findLaunchNews(NewsSection.BUSINESS, NEWS_DATE))
            .willReturn(List.of(new DailyDigestSourceItem("공기청정기 출시", "요약")));
        given(newsPosts.digestExists(NewsSection.BUSINESS, NEWS_DATE)).willReturn(true);
        given(draftGenerator.generate(any()))
            .willReturn(Optional.of(new DailyDigestDraft("디지털 브리핑 본문", List.of())));
        given(newsPosts.publishDailyDigest(any(), any(), any())).willReturn(11L);

        DailyDigestPublishResult result = useCase.publish(NEWS_DATE);

        assertThat(result.createdPostIds()).containsExactly(11L);
        assertThat(result.skips()).containsEntry(NewsSection.BUSINESS, SkipReason.ALREADY_PUBLISHED);
        assertThat(result.skips()).doesNotContainKey(NewsSection.TECHNOLOGY);
        assertThat(result.newsDate()).isEqualTo(NEWS_DATE);
    }

    private DailyDigestDraftCommand argThatSection(NewsSection section) {
        return argThat(command -> command != null && command.category() == section);
    }
}
