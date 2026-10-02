package dev.iamrat.ingest.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.board.post.LaunchNewsPost;
import dev.iamrat.core.board.post.LaunchNewsPostDraft;
import dev.iamrat.core.board.post.LaunchNewsPostDraftCommand;
import dev.iamrat.core.board.post.LaunchNewsPostDraftGenerator;
import dev.iamrat.core.board.post.NewsPostPort;
import dev.iamrat.core.board.post.NewsSection;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.ingest.news.application.LaunchNewsPublishResult.Skip;
import dev.iamrat.ingest.news.application.LaunchNewsPublishResult.SkipReason;
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
    private NewsPostPort newsPosts;

    private PublishLaunchNewsUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-21T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        useCase = new PublishLaunchNewsUseCase(
            ingestProductNewsUseCase,
            new LaunchNewsEligibilityPolicy(),
            draftGenerator,
            newsPosts,
            clock
        );
    }

    @Test
    @DisplayName("모든 게이트를 통과한 뒤에만 출시 뉴스를 게시한다")
    void publishesLaunchNewsOnlyAfterAllGatesPass() {
        givenCollected(List.of(validItem()));
        given(newsPosts.isPublished("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(newsPosts.publishLaunchNews(any(LaunchNewsPost.class))).willReturn(42L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.createdPostIds()).containsExactly(42L);

        LaunchNewsPost published = capturePublished();
        assertThat(published.draft().title()).isEqualTo("갤럭시북 신제품 출시");
        assertThat(published.publishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
        assertThat(published.keyword()).isEqualTo("갤럭시북");
        assertThat(published.sourceTitle()).isEqualTo("갤럭시북 신제품 출시");
        assertThat(published.canonicalUrl()).isEqualTo("https://n.news.naver.com/article/001/1");
        assertThat(published.originalUrl()).isEqualTo("https://n.news.naver.com/article/001/1?utm=1");
        assertThat(published.sourceName()).isEqualTo("n.news.naver.com");
        assertThat(published.publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 21, 10, 0));
    }

    @Test
    @DisplayName("중복 기사면 AI 생성 전에 건너뛴다")
    void rejectsDuplicateArticleBeforeAiGeneration() {
        givenCollected(List.of(validItem()));
        given(newsPosts.isPublished("https://n.news.naver.com/article/001/1")).willReturn(true);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isZero();
        assertThat(result.skips()).extracting(Skip::reason)
            .containsExactly(SkipReason.DUPLICATE_ARTICLE);
        verify(draftGenerator, never()).generate(any());
        verify(newsPosts, never()).publishLaunchNews(any());
    }

    @Test
    @DisplayName("AI 생성이 실패하면 후보를 건너뛴다")
    void skipsCandidateWhenAiGenerationFails() {
        givenCollected(List.of(validItem()));
        given(newsPosts.isPublished("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.empty());

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isZero();
        assertThat(result.skips()).extracting(Skip::reason)
            .containsExactly(SkipReason.AI_GENERATION_FAILED);
        verify(newsPosts, never()).publishLaunchNews(any());
    }

    @Test
    @DisplayName("일일 한도를 초과하면 AI 생성 전에 건너뛴다")
    void skipsWhenDailyCapExceededBeforeAiGeneration() {
        givenCollected(List.of(validItem()));
        given(newsPosts.isPublished("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(3L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isZero();
        assertThat(result.skips()).extracting(Skip::reason)
            .containsExactly(SkipReason.DAILY_CAP_EXCEEDED);
        verify(draftGenerator, never()).generate(any());
    }

    @Test
    @DisplayName("같은 배치 중복은 기계가 읽을 수 있는 skip으로 기록한다")
    void recordsSameBatchDuplicateAsMachineReadableSkip() {
        givenCollected(List.of(validItem(), validItem()));
        given(newsPosts.isPublished("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(newsPosts.publishLaunchNews(any(LaunchNewsPost.class))).willReturn(42L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.skips()).extracting(Skip::reason)
            .containsExactly(SkipReason.DUPLICATE_ARTICLE);
        verify(newsPosts, times(1)).isPublished("https://n.news.naver.com/article/001/1");
        verify(draftGenerator, times(1)).generate(any());
        verify(newsPosts, times(1)).publishLaunchNews(any());
    }

    @Test
    @DisplayName("원본 발행 시간을 파싱할 수 없으면 처리 시간을 저장한다")
    void storesProcessingTimeWhenSourcePublishedAtCannotBeParsed() {
        givenCollected(List.of(itemWithInvalidPublishedAt()));
        given(newsPosts.isPublished("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(newsPosts.publishLaunchNews(any(LaunchNewsPost.class))).willReturn(42L);

        useCase.publish(command(3));

        assertThat(capturePublished().publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 21, 9, 0));
    }

    @Test
    @DisplayName("해외 원본 발행 시간을 서비스 타임존으로 변환한다")
    void convertsSourcePublishedAtToServiceTimeZone() {
        givenCollected(List.of(itemWithOverseasPublishedAt()));
        given(newsPosts.isPublished("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 22)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(newsPosts.publishLaunchNews(any(LaunchNewsPost.class))).willReturn(42L);

        useCase.publish(command(3));

        assertThat(capturePublished().publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 22, 9, 0));
    }

    @Test
    @DisplayName("저장 중 유니크 제약 충돌은 해당 기사만 중복 skip으로 격리한다")
    void isolatesUniqueConstraintViolationAsDuplicateSkipForThatArticleOnly() {
        givenCollected(List.of(validItem(), secondValidItem()));
        given(newsPosts.isPublished(any())).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(newsPosts.publishLaunchNews(any(LaunchNewsPost.class)))
            .willReturn(42L)
            .willThrow(new DataIntegrityViolationException("duplicate canonical_url"));

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.createdPostIds()).containsExactly(42L);
        assertThat(result.skips()).extracting(Skip::reason)
            .containsExactly(SkipReason.DUPLICATE_ARTICLE);
    }

    @Test
    @DisplayName("커맨드의 분야가 게시 요청에 실린다")
    void carriesCommandSectionIntoPublishedNews() {
        givenCollected(List.of(validItem()));
        given(newsPosts.isPublished("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(newsPosts.publishLaunchNews(any(LaunchNewsPost.class))).willReturn(42L);

        useCase.publish(new LaunchNewsPublishCommand(
            "갤럭시북",
            5,
            3,
            List.of("출시"),
            NewsSection.TECHNOLOGY,
            PostPublishOrigin.SYSTEM_BATCH
        ));

        assertThat(capturePublished().section()).isEqualTo(NewsSection.TECHNOLOGY);
    }

    @Test
    @DisplayName("URL을 URI로 파싱할 수 없으면 query string만 잘라내고 출처 호스트는 빈 값으로 처리한다")
    void fallsBackToQueryStrippedUrlAndBlankHostWhenUrlIsUnparseable() {
        givenCollected(List.of(itemWithUnparseableUrl()));
        given(newsPosts.isPublished("https://n.news.naver.com/article/001/1 launch"))
            .willReturn(false);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isZero();
        assertThat(result.skips()).hasSize(1);
        assertThat(result.skips().getFirst().url())
            .isEqualTo("https://n.news.naver.com/article/001/1 launch");
        assertThat(result.skips().getFirst().reason()).isEqualTo(SkipReason.UNKNOWN_SOURCE);
        verify(draftGenerator, never()).generate(any());
    }

    @Test
    @DisplayName("topics가 비어 있으면 주제를 붙이지 않고 키워드만 넘긴다")
    void passesEmptyTopicsThroughWithoutDefaults() {
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
            List.of()
        );
    }

    @Test
    @DisplayName("기사 번호가 query에 있는 언론사는 서로 다른 기사로 구분한다")
    void distinguishesArticlesWhoseIdLivesInTheQueryString() {
        givenCollected(List.of(
                pressItemWithQueryId("20260611000123"),
                pressItemWithQueryId("20260611000999")
            ));
        given(newsPosts.isPublished(any())).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(newsPosts.publishLaunchNews(any(LaunchNewsPost.class))).willReturn(42L, 43L);

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
        given(newsPosts.isPublished(any())).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(newsPosts.publishLaunchNews(any(LaunchNewsPost.class))).willReturn(42L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skips()).extracting(Skip::reason)
            .containsExactly(SkipReason.DUPLICATE_ARTICLE);
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
        given(newsPosts.isPublished(any())).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(newsPosts.publishLaunchNews(any(LaunchNewsPost.class))).willReturn(42L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skips()).extracting(Skip::reason)
            .containsExactly(SkipReason.DUPLICATE_ARTICLE);
    }

    @Test
    @DisplayName("URI로 파싱할 수 없는 주소도 기사 번호는 유지하고 추적 파라미터만 제거한다")
    void keepsArticleIdForUnparseableUrlsWhileStrippingTrackers() {
        givenCollected(List.of(
                pressItem("https://www.etnews.com/a.html?id=1|x&utm_source=naver"),
                pressItem("https://www.etnews.com/a.html?id=2|x&utm_source=naver")
            ));
        given(newsPosts.isPublished(any())).willReturn(false);
        given(newsPosts.countPublished("갤럭시북", LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북", "launch-news"))
        ));
        given(newsPosts.publishLaunchNews(any(LaunchNewsPost.class))).willReturn(42L, 43L);

        LaunchNewsPublishResult result = useCase.publish(command(3));

        assertThat(result.publishedCount()).isEqualTo(2);
        assertThat(result.skips()).isEmpty();
    }

    private LaunchNewsPost capturePublished() {
        ArgumentCaptor<LaunchNewsPost> captor = ArgumentCaptor.forClass(LaunchNewsPost.class);
        verify(newsPosts).publishLaunchNews(captor.capture());
        return captor.getValue();
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
