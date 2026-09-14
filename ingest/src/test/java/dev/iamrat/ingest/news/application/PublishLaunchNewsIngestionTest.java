package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.board.post.LaunchNewsPostDraft;
import dev.iamrat.core.board.post.LaunchNewsPostDraftCommand;
import dev.iamrat.core.board.post.LaunchNewsPostDraftGenerator;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostReferenceLinkReader;
import dev.iamrat.core.board.post.PostReferenceLinkWriter;
import dev.iamrat.core.board.post.PostWriteCommand;
import dev.iamrat.core.board.post.PostWriter;
import dev.iamrat.ingest.document.application.DocumentIngestResult;
import dev.iamrat.ingest.document.application.IngestDocumentsUseCase;
import dev.iamrat.source.news.application.NewsSourceClient;
import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishLaunchNewsIngestionTest {

    @Mock private NewsSourceClient newsSourceClient;
    @Mock private IngestDocumentsUseCase ingestDocumentsUseCase;
    @Mock private LaunchNewsPostDraftGenerator draftGenerator;
    @Mock private PostReferenceLinkReader referenceLinkReader;
    @Mock private PostWriter postWriter;
    @Mock private PostReferenceLinkWriter referenceLinkWriter;

    private PublishLaunchNewsUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-21T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        IngestProductNewsUseCase ingestProductNewsUseCase = new IngestProductNewsUseCase(
            ingestDocumentsUseCase,
            newsSourceClient,
            new SimpleMeterRegistry()
        );
        useCase = new PublishLaunchNewsUseCase(
            ingestProductNewsUseCase,
            new LaunchNewsEligibilityPolicy(),
            draftGenerator,
            referenceLinkReader,
            new LaunchNewsPostRecorder(postWriter, referenceLinkWriter, clock),
            clock
        );
    }

    @Test
    @DisplayName("한 번 수집한 원본 기사를 먼저 저장한 뒤 초안을 생성한다")
    void persistsCollectedArticleBeforeDraftingWithoutFetchingTwice() {
        given(newsSourceClient.search(any())).willReturn(List.of(item(), item()));
        given(ingestDocumentsUseCase.ingest(any())).willReturn(new DocumentIngestResult(1, 1));
        given(referenceLinkReader.existsByCanonicalUrl(any())).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21))).willReturn(0L);
        given(draftGenerator.generate(any())).willReturn(Optional.of(draft()));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        LaunchNewsPublishResult result = useCase.publish(command());

        assertThat(result.createdPostIds()).containsExactly(42L);
        assertThat(result.skips()).extracting(LaunchNewsSkip::reason)
            .containsExactly(LaunchNewsSkipReason.DUPLICATE_ARTICLE);
        InOrder order = inOrder(ingestDocumentsUseCase, draftGenerator);
        order.verify(ingestDocumentsUseCase).ingest(any());
        order.verify(draftGenerator).generate(any(LaunchNewsPostDraftCommand.class));
        verify(newsSourceClient).search(new NewsSourceQuery("갤럭시북 출시", 5, "date"));
    }

    @Test
    @DisplayName("문서 저장 실패 시 초안과 게시글을 만들지 않는다")
    void persistenceFailurePreventsDraftAndPost() {
        given(newsSourceClient.search(any())).willReturn(List.of(item()));
        given(ingestDocumentsUseCase.ingest(any())).willThrow(new IllegalStateException("vector store unavailable"));

        assertThatThrownBy(() -> useCase.publish(command()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("vector store unavailable");
        verify(draftGenerator, never()).generate(any());
        verify(postWriter, never()).write(any());
    }

    @Test
    @DisplayName("초안 실패 후 재실행하면 미게시 기사를 게시할 수 있다")
    void rerunAfterDraftFailurePublishesUnrecordedArticle() {
        given(newsSourceClient.search(any())).willReturn(List.of(item()));
        given(ingestDocumentsUseCase.ingest(any())).willReturn(new DocumentIngestResult(1, 1));
        given(referenceLinkReader.existsByCanonicalUrl(any())).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21))).willReturn(0L);
        given(draftGenerator.generate(any()))
            .willReturn(Optional.empty())
            .willReturn(Optional.of(draft()));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        LaunchNewsPublishResult first = useCase.publish(command());
        LaunchNewsPublishResult second = useCase.publish(command());

        assertThat(first.createdPostIds()).isEmpty();
        assertThat(second.createdPostIds()).containsExactly(42L);
        verify(referenceLinkWriter, times(1)).write(any());
    }

    @Test
    @DisplayName("이미 게시된 URL은 재실행에서 다시 초안을 만들지 않는다")
    void repeatRunSkipsAlreadyPublishedUrl() {
        given(newsSourceClient.search(any())).willReturn(List.of(item()));
        given(ingestDocumentsUseCase.ingest(any())).willReturn(new DocumentIngestResult(1, 1));
        given(referenceLinkReader.existsByCanonicalUrl(any())).willReturn(false, true);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21))).willReturn(0L);
        given(draftGenerator.generate(any())).willReturn(Optional.of(draft()));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        useCase.publish(command());
        LaunchNewsPublishResult repeated = useCase.publish(command());

        assertThat(repeated.createdPostIds()).isEmpty();
        assertThat(repeated.skips()).extracting(LaunchNewsSkip::reason)
            .containsExactly(LaunchNewsSkipReason.DUPLICATE_ARTICLE);
        verify(draftGenerator, times(1)).generate(any());
    }

    private LaunchNewsPublishCommand command() {
        return new LaunchNewsPublishCommand(
            "갤럭시북", 5, 3, List.of("출시"), null, PostPublishOrigin.SYSTEM_BATCH
        );
    }

    private NewsSourceItem item() {
        return new NewsSourceItem(
            "갤럭시북 신제품 출시",
            "삼성이 갤럭시북 신제품을 공개했다.",
            "https://n.news.naver.com/article/001/1",
            "https://n.news.naver.com/article/001/1?utm_source=naver",
            "Sun, 21 Jun 2026 10:00:00 +0900"
        );
    }

    private LaunchNewsPostDraft draft() {
        return new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북"));
    }
}
