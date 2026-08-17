package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.board.post.LaunchNewsPostDraft;
import dev.iamrat.core.board.post.LaunchNewsPostDraftCommand;
import dev.iamrat.core.board.post.LaunchNewsPostDraftGenerator;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostReferenceLinkCommand;
import dev.iamrat.core.board.post.PostReferenceLinkReader;
import dev.iamrat.core.board.post.PostReferenceLinkWriter;
import dev.iamrat.core.board.post.PostWriteCommand;
import dev.iamrat.core.board.post.PostWriter;
import dev.iamrat.source.news.application.NewsSourceClient;
import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import dev.iamrat.source.news.application.NewsSourceResult;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishLaunchNewsUseCaseTest {

    @Mock
    private NewsSourceClient newsSourceClient;

    @Mock
    private LaunchNewsPostDraftGenerator draftGenerator;

    @Mock
    private PostWriter postWriter;

    @Mock
    private PostReferenceLinkReader referenceLinkReader;

    @Mock
    private PostReferenceLinkWriter referenceLinkWriter;

    private PublishLaunchNewsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PublishLaunchNewsUseCase(
            newsSourceClient,
            new LaunchNewsEligibilityPolicy(),
            draftGenerator,
            postWriter,
            referenceLinkReader,
            referenceLinkWriter,
            Clock.fixed(Instant.parse("2026-06-21T00:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    @Test
    @DisplayName("모든 게이트를 통과한 뒤에만 게시글과 참조 링크를 저장한다")
    void writesBoardPostAndReferenceOnlyAfterAllGatesPass() {
        given(newsSourceClient.search(any(NewsSourceQuery.class))).willReturn(new NewsSourceResult(List.of(validItem())));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordAndProductIdOnDate("갤럭시북", 10L, LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.createdPostIds()).containsExactly(42L);

        ArgumentCaptor<PostWriteCommand> postCommand = ArgumentCaptor.forClass(PostWriteCommand.class);
        verify(postWriter).write(postCommand.capture());
        assertThat(postCommand.getValue().category()).isEqualTo(PostCategory.PRODUCT_LAUNCH_NEWS);
        assertThat(postCommand.getValue().publishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);

        ArgumentCaptor<PostReferenceLinkCommand> referenceCommand =
            ArgumentCaptor.forClass(PostReferenceLinkCommand.class);
        verify(referenceLinkWriter).write(referenceCommand.capture());
        assertThat(referenceCommand.getValue().postId()).isEqualTo(42L);
        assertThat(referenceCommand.getValue().keyword()).isEqualTo("갤럭시북");
        assertThat(referenceCommand.getValue().productId()).isEqualTo(10L);
        assertThat(referenceCommand.getValue().canonicalUrl())
            .isEqualTo("https://n.news.naver.com/article/001/1");
        assertThat(referenceCommand.getValue().publishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
    }

    @Test
    @DisplayName("중복 기사면 AI 생성 전에 건너뛴다")
    void rejectsDuplicateArticleBeforeAiGeneration() {
        given(newsSourceClient.search(any(NewsSourceQuery.class))).willReturn(new NewsSourceResult(List.of(validItem())));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(true);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isZero();
        assertThat(result.skips()).extracting(LaunchNewsSkip::reason)
            .containsExactly(LaunchNewsSkipReason.DUPLICATE_ARTICLE);
        verify(draftGenerator, never()).generate(any());
        verify(postWriter, never()).write(any());
    }

    @Test
    @DisplayName("AI 생성이 실패하면 후보를 건너뛴다")
    void skipsCandidateWhenAiGenerationFails() {
        given(newsSourceClient.search(any(NewsSourceQuery.class))).willReturn(new NewsSourceResult(List.of(validItem())));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordAndProductIdOnDate("갤럭시북", 10L, LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.empty());

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isZero();
        assertThat(result.skips()).extracting(LaunchNewsSkip::reason)
            .containsExactly(LaunchNewsSkipReason.AI_GENERATION_FAILED);
        verify(postWriter, never()).write(any());
        verify(referenceLinkWriter, never()).write(any());
    }

    @Test
    @DisplayName("일일 한도를 초과하면 AI 생성 전에 건너뛴다")
    void skipsWhenDailyCapExceededBeforeAiGeneration() {
        given(newsSourceClient.search(any(NewsSourceQuery.class))).willReturn(new NewsSourceResult(List.of(validItem())));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordAndProductIdOnDate("갤럭시북", 10L, LocalDate.of(2026, 6, 21)))
            .willReturn(3L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isZero();
        assertThat(result.skips()).extracting(LaunchNewsSkip::reason)
            .containsExactly(LaunchNewsSkipReason.DAILY_CAP_EXCEEDED);
        verify(draftGenerator, never()).generate(any());
    }

    @Test
    @DisplayName("중복 확인에는 정규화된 canonical 뉴스 URL을 사용한다")
    void normalizesCanonicalNewsUrlForDuplicateChecks() {
        given(newsSourceClient.search(any(NewsSourceQuery.class))).willReturn(new NewsSourceResult(List.of(validItem())));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(true);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.skips().getFirst().url()).isEqualTo("https://n.news.naver.com/article/001/1");
        verify(referenceLinkReader).existsByCanonicalUrl("https://n.news.naver.com/article/001/1");
    }

    @Test
    @DisplayName("같은 배치 중복은 기계가 읽을 수 있는 skip으로 기록한다")
    void recordsSameBatchDuplicateAsMachineReadableSkip() {
        given(newsSourceClient.search(any(NewsSourceQuery.class)))
            .willReturn(new NewsSourceResult(List.of(validItem(), validItem())));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordAndProductIdOnDate("갤럭시북", 10L, LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.skips()).extracting(LaunchNewsSkip::reason)
            .containsExactly(LaunchNewsSkipReason.DUPLICATE_ARTICLE);
        verify(referenceLinkReader, times(1)).existsByCanonicalUrl("https://n.news.naver.com/article/001/1");
        verify(draftGenerator, times(1)).generate(any());
        verify(postWriter, times(1)).write(any());
    }

    @Test
    @DisplayName("원본 발행 시간을 파싱할 수 없으면 처리 시간을 저장한다")
    void storesProcessingTimeWhenSourcePublishedAtCannotBeParsed() {
        given(newsSourceClient.search(any(NewsSourceQuery.class)))
            .willReturn(new NewsSourceResult(List.of(itemWithInvalidPublishedAt())));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordAndProductIdOnDate("갤럭시북", 10L, LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        useCase.publish(command(3));

        ArgumentCaptor<PostReferenceLinkCommand> referenceCommand =
            ArgumentCaptor.forClass(PostReferenceLinkCommand.class);
        verify(referenceLinkWriter).write(referenceCommand.capture());
        assertThat(referenceCommand.getValue().publishedAt())
            .isEqualTo(LocalDateTime.of(2026, 6, 21, 9, 0));
    }

    @Test
    @DisplayName("해외 원본 발행 시간을 서비스 타임존으로 변환한다")
    void convertsSourcePublishedAtToServiceTimeZone() {
        given(newsSourceClient.search(any(NewsSourceQuery.class)))
            .willReturn(new NewsSourceResult(List.of(itemWithOverseasPublishedAt())));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordAndProductIdOnDate("갤럭시북", 10L, LocalDate.of(2026, 6, 22)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        useCase.publish(command(3));

        ArgumentCaptor<PostReferenceLinkCommand> referenceCommand =
            ArgumentCaptor.forClass(PostReferenceLinkCommand.class);
        verify(referenceLinkWriter).write(referenceCommand.capture());
        assertThat(referenceCommand.getValue().publishedAt())
            .isEqualTo(LocalDateTime.of(2026, 6, 22, 9, 0));
    }

    private LaunchNewsPublishCommand command(int dailyCap) {
        return new LaunchNewsPublishCommand(
            "갤럭시북",
            10L,
            5,
            dailyCap,
            List.of("출시"),
            PostPublishOrigin.SYSTEM_BATCH
        );
    }

    private NewsSourceItem validItem() {
        return new NewsSourceItem(
            "갤럭시북 신제품 출시",
            "삼성이 갤럭시북 신제품을 공개했다.",
            "https://n.news.naver.com/article/001/1?ntype=RANKING",
            "https://n.news.naver.com/article/001/1?utm=1",
            "Sun, 21 Jun 2026 10:00:00 +0900"
        );
    }

    private NewsSourceItem itemWithInvalidPublishedAt() {
        return new NewsSourceItem(
            "갤럭시북 신제품 출시",
            "삼성이 갤럭시북 신제품을 공개했다.",
            "https://n.news.naver.com/article/001/1?ntype=RANKING",
            "https://n.news.naver.com/article/001/1?utm=1",
            "not-a-date"
        );
    }

    private NewsSourceItem itemWithOverseasPublishedAt() {
        return new NewsSourceItem(
            "갤럭시북 신제품 출시",
            "삼성이 갤럭시북 신제품을 공개했다.",
            "https://n.news.naver.com/article/001/1?ntype=RANKING",
            "https://n.news.naver.com/article/001/1?utm=1",
            "Sun, 21 Jun 2026 20:00:00 -0400"
        );
    }
}
