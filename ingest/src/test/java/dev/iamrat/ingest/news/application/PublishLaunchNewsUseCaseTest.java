package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.board.post.BoardCategory;
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
import dev.iamrat.source.news.application.NewsSourceItem;
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
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PublishLaunchNewsUseCaseTest {

    @Mock
    private IngestProductNewsUseCase ingestProductNewsUseCase;

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
        Clock clock = Clock.fixed(Instant.parse("2026-06-21T00:00:00Z"), ZoneId.of("Asia/Seoul"));
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
    @DisplayName("모든 게이트를 통과한 뒤에만 게시글과 참조 링크를 저장한다")
    void writesBoardPostAndReferenceOnlyAfterAllGatesPass() {
        givenCollected(List.of(validItem()));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21)))
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
        assertThat(referenceCommand.getValue().canonicalUrl())
            .isEqualTo("https://n.news.naver.com/article/001/1");
        assertThat(referenceCommand.getValue().publishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
    }

    @Test
    @DisplayName("중복 기사면 AI 생성 전에 건너뛴다")
    void rejectsDuplicateArticleBeforeAiGeneration() {
        givenCollected(List.of(validItem()));
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
        givenCollected(List.of(validItem()));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21)))
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
        givenCollected(List.of(validItem()));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(3L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isZero();
        assertThat(result.skips()).extracting(LaunchNewsSkip::reason)
            .containsExactly(LaunchNewsSkipReason.DAILY_CAP_EXCEEDED);
        verify(draftGenerator, never()).generate(any());
    }

    @Test
    @DisplayName("같은 배치 중복은 기계가 읽을 수 있는 skip으로 기록한다")
    void recordsSameBatchDuplicateAsMachineReadableSkip() {
        givenCollected(List.of(validItem(), validItem()));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21)))
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
        givenCollected(List.of(itemWithInvalidPublishedAt()));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21)))
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
        givenCollected(List.of(itemWithOverseasPublishedAt()));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 22)))
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

    @Test
    @DisplayName("저장 중 유니크 제약 충돌은 해당 기사만 중복 skip으로 격리한다")
    void isolatesUniqueConstraintViolationAsDuplicateSkipForThatArticleOnly() {
        givenCollected(List.of(validItem(), secondValidItem()));
        given(referenceLinkReader.existsByCanonicalUrl(any())).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(postWriter.write(any(PostWriteCommand.class)))
            .willReturn(42L)
            .willThrow(new DataIntegrityViolationException("duplicate canonical_url"));

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.createdPostIds()).containsExactly(42L);
        assertThat(result.skips()).extracting(LaunchNewsSkip::reason)
            .containsExactly(LaunchNewsSkipReason.DUPLICATE_ARTICLE);
    }

    @Test
    @DisplayName("커맨드의 분야 카테고리가 게시글 작성 커맨드에 실린다")
    void carriesCommandCategoryIntoPostWriteCommand() {
        givenCollected(List.of(validItem()));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        useCase.publish(new LaunchNewsPublishCommand(
            "갤럭시북",
            5,
            3,
            List.of("출시"),
            BoardCategory.DIGITAL,
            PostPublishOrigin.SYSTEM_BATCH
        ));

        ArgumentCaptor<PostWriteCommand> postCommand = ArgumentCaptor.forClass(PostWriteCommand.class);
        verify(postWriter).write(postCommand.capture());
        assertThat(postCommand.getValue().boardCategory()).isEqualTo(BoardCategory.DIGITAL);
    }

    @Test
    @DisplayName("URL을 URI로 파싱할 수 없으면 query string만 잘라내고 출처 호스트는 빈 값으로 처리한다")
    void fallsBackToQueryStrippedUrlAndBlankHostWhenUrlIsUnparseable() {
        givenCollected(List.of(itemWithUnparseableUrl()));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1 launch"))
            .willReturn(false);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isZero();
        assertThat(result.skips()).hasSize(1);
        assertThat(result.skips().getFirst().url())
            .isEqualTo("https://n.news.naver.com/article/001/1 launch");
        assertThat(result.skips().getFirst().reason()).isEqualTo(LaunchNewsSkipReason.UNKNOWN_SOURCE);
        verify(draftGenerator, never()).generate(any());
    }

    @Test
    @DisplayName("topics가 비어 있으면 기본 주제로 검색한다")
    void searchesDefaultTopicsWhenTopicsIsEmpty() {
        givenCollected(List.of());

        LaunchNewsPublishResult result = useCase.publish(new LaunchNewsPublishCommand(
            "갤럭시북",
            5,
            3,
            List.of(),
            null,
            PostPublishOrigin.SYSTEM_BATCH
        ));

        assertThat(result.publishedCount()).isZero();
        assertThat(result.skippedCount()).isZero();
        verify(ingestProductNewsUseCase).collectAndIngest(
            "갤럭시북",
            5,
            List.of("신제품", "출시", "공개", "사전예약")
        );
    }

    @Test
    @DisplayName("기사 번호가 query에 있는 언론사는 서로 다른 기사로 구분한다")
    void distinguishesArticlesWhoseIdLivesInTheQueryString() {
        givenCollected(List.of(
                pressItemWithQueryId("20260611000123"),
                pressItemWithQueryId("20260611000999")
            ));
        given(referenceLinkReader.existsByCanonicalUrl(any())).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L, 43L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isEqualTo(2);
        assertThat(result.skips()).isEmpty();
    }

    @Test
    @DisplayName("같은 기사의 추적 파라미터 변형은 중복으로 본다")
    void treatsTrackingParameterVariantsOfTheSameArticleAsDuplicate() {
        givenCollected(List.of(
                pressItemWithQueryId("20260611000123"),
                pressItem("https://www.etnews.com/news/article.html?id=20260611000123&utm_source=naver")
            ));
        given(referenceLinkReader.existsByCanonicalUrl(any())).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skips()).extracting(LaunchNewsSkip::reason)
            .containsExactly(LaunchNewsSkipReason.DUPLICATE_ARTICLE);
        assertThat(result.skips().getFirst().url())
            .isEqualTo("https://www.etnews.com/news/article.html?id=20260611000123");
    }

    @Test
    @DisplayName("query 파라미터 순서가 달라도 같은 기사로 본다")
    void treatsSameArticleWithReorderedQueryParametersAsDuplicate() {
        givenCollected(List.of(
                pressItem("https://www.etnews.com/news/article.html?cid=7&id=123"),
                pressItem("https://www.etnews.com/news/article.html?id=123&cid=7")
            ));
        given(referenceLinkReader.existsByCanonicalUrl(any())).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skips()).extracting(LaunchNewsSkip::reason)
            .containsExactly(LaunchNewsSkipReason.DUPLICATE_ARTICLE);
    }

    @Test
    @DisplayName("URI로 파싱할 수 없는 주소도 기사 번호는 유지하고 추적 파라미터만 제거한다")
    void keepsArticleIdForUnparseableUrlsWhileStrippingTrackers() {
        givenCollected(List.of(
                pressItem("https://www.etnews.com/a.html?id=1|x&utm_source=naver"),
                pressItem("https://www.etnews.com/a.html?id=2|x&utm_source=naver")
            ));
        given(referenceLinkReader.existsByCanonicalUrl(any())).willReturn(false);
        given(referenceLinkReader.countByKeywordOnDate("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L, 43L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isEqualTo(2);
        assertThat(result.skips()).isEmpty();
    }

    private NewsSourceItem pressItemWithQueryId(String articleId) {
        return pressItem("https://www.etnews.com/news/article.html?id=" + articleId);
    }

    private void givenCollected(List<NewsSourceItem> items) {
        given(ingestProductNewsUseCase.collectAndIngest(any(), any(), any()))
            .willReturn(new IngestProductNewsUseCase.CollectedNews(null, items));
    }

    private NewsSourceItem pressItem(String url) {
        return new NewsSourceItem(
            "갤럭시북 신제품 출시",
            "삼성이 갤럭시북 신제품을 공개했다.",
            "https://n.news.naver.com/article/001/" + url.hashCode(),
            url,
            "Sun, 21 Jun 2026 10:00:00 +0900"
        );
    }

    private LaunchNewsPublishCommand command(int dailyCap) {
        return new LaunchNewsPublishCommand(
            "갤럭시북",
            5,
            dailyCap,
            List.of("출시"),
            null,
            PostPublishOrigin.SYSTEM_BATCH
        );
    }

    private NewsSourceItem itemWithUnparseableUrl() {
        return new NewsSourceItem(
            "갤럭시북 신제품 출시",
            "삼성이 갤럭시북 신제품을 공개했다.",
            "https://n.news.naver.com/article/001/1 launch?utm=1",
            "https://n.news.naver.com/article/001/1 launch?utm=1",
            "Sun, 21 Jun 2026 10:00:00 +0900"
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

    private NewsSourceItem secondValidItem() {
        return new NewsSourceItem(
            "갤럭시북 프로 출시 공개",
            "삼성이 갤럭시북 프로를 출시했다.",
            "https://n.news.naver.com/article/001/2?ntype=RANKING",
            "https://n.news.naver.com/article/001/2?utm=1",
            "Sun, 21 Jun 2026 11:00:00 +0900"
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
