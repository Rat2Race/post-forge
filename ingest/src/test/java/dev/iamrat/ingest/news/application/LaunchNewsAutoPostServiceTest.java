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
import dev.iamrat.core.board.post.PostBoardCategory;
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
class LaunchNewsAutoPostServiceTest {

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

    private LaunchNewsAutoPostService service;

    @BeforeEach
    void setUp() {
        service = new LaunchNewsAutoPostService(
            newsSourceClient,
            new LaunchNewsGatePolicy(),
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

        LaunchNewsAutoPostResult result = service.postLaunchNews(request(3));

        assertThat(result.acceptedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.createdPostIds()).containsExactly(42L);

        ArgumentCaptor<PostWriteCommand> postCommand = ArgumentCaptor.forClass(PostWriteCommand.class);
        verify(postWriter).write(postCommand.capture());
        assertThat(postCommand.getValue().category()).isEqualTo(PostCategory.PRODUCT_LAUNCH_NEWS);
        assertThat(postCommand.getValue().boardCategory()).isEqualTo(PostBoardCategory.DIGITAL);
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
    @DisplayName("짧은 디지털 토큰보다 헬스 카테고리를 우선 판별한다")
    void resolvesHealthBeforeShortDigitalToken() {
        assertResolvedBoardCategory(
            newsItem("AI fitness band 출시", "fitness health tracker 공개"),
            new LaunchNewsPostDraft("AI fitness band", "health tracker", "요약", List.of("fitness")),
            PostBoardCategory.HEALTH
        );
    }

    @Test
    @DisplayName("짧은 디지털 토큰보다 가전 카테고리를 우선 판별한다")
    void resolvesApplianceBeforeShortDigitalToken() {
        assertResolvedBoardCategory(
            newsItem("smart kitchen appliance 출시", "home living product 공개"),
            new LaunchNewsPostDraft("smart kitchen appliance", "home living", "요약", List.of("kitchen")),
            PostBoardCategory.APPLIANCE
        );
    }

    @Test
    @DisplayName("생활 키워드는 생활 카테고리로 판별한다")
    void resolvesLivingBoardCategory() {
        assertResolvedBoardCategory(
            newsItem("living furniture 출시", "home living product 공개"),
            new LaunchNewsPostDraft("living furniture", "home living", "요약", List.of("living")),
            PostBoardCategory.LIVING
        );
    }

    @Test
    @DisplayName("스포츠 키워드는 스포츠 카테고리로 판별한다")
    void resolvesSportsBoardCategory() {
        assertResolvedBoardCategory(
            newsItem("sports watch 출시", "sports outdoor product 공개"),
            new LaunchNewsPostDraft("sports watch", "sports outdoor", "요약", List.of("sports")),
            PostBoardCategory.SPORTS
        );
    }

    @Test
    @DisplayName("구체적인 뷰티 키워드가 있으면 디지털보다 뷰티 카테고리를 우선한다")
    void resolvesBeautyBeforeDigitalWhenSpecificKeywordExists() {
        assertResolvedBoardCategory(
            newsItem("AI skincare device 출시", "beauty cosmetic product 공개"),
            new LaunchNewsPostDraft("AI skincare device", "beauty cosmetic", "요약", List.of("skincare")),
            PostBoardCategory.BEAUTY
        );
    }

    @Test
    @DisplayName("중복 기사면 AI 생성 전에 건너뛴다")
    void rejectsDuplicateArticleBeforeAiGeneration() {
        given(newsSourceClient.search(any(NewsSourceQuery.class))).willReturn(new NewsSourceResult(List.of(validItem())));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(true);

        LaunchNewsAutoPostResult result = service.postLaunchNews(request(3));

        assertThat(result.acceptedCount()).isZero();
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

        LaunchNewsAutoPostResult result = service.postLaunchNews(request(3));

        assertThat(result.acceptedCount()).isZero();
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

        LaunchNewsAutoPostResult result = service.postLaunchNews(request(3));

        assertThat(result.acceptedCount()).isZero();
        assertThat(result.skips()).extracting(LaunchNewsSkip::reason)
            .containsExactly(LaunchNewsSkipReason.DAILY_CAP_EXCEEDED);
        verify(draftGenerator, never()).generate(any());
    }

    @Test
    @DisplayName("중복 확인에는 정규화된 canonical 뉴스 URL을 사용한다")
    void normalizesCanonicalNewsUrlForDuplicateChecks() {
        given(newsSourceClient.search(any(NewsSourceQuery.class))).willReturn(new NewsSourceResult(List.of(validItem())));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(true);

        LaunchNewsAutoPostResult result = service.postLaunchNews(request(3));

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

        LaunchNewsAutoPostResult result = service.postLaunchNews(request(3));

        assertThat(result.acceptedCount()).isEqualTo(1);
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

        service.postLaunchNews(request(3));

        ArgumentCaptor<PostReferenceLinkCommand> referenceCommand =
            ArgumentCaptor.forClass(PostReferenceLinkCommand.class);
        verify(referenceLinkWriter).write(referenceCommand.capture());
        assertThat(referenceCommand.getValue().publishedAt())
            .isEqualTo(LocalDateTime.of(2026, 6, 21, 9, 0));
    }

    private LaunchNewsAutoPostRequest request(int dailyCap) {
        return new LaunchNewsAutoPostRequest(
            "갤럭시북",
            10L,
            5,
            dailyCap,
            List.of("출시"),
            PostPublishOrigin.SYSTEM_BATCH
        );
    }

    private void assertResolvedBoardCategory(
        NewsSourceItem item,
        LaunchNewsPostDraft draft,
        PostBoardCategory expected
    ) {
        given(newsSourceClient.search(any(NewsSourceQuery.class))).willReturn(new NewsSourceResult(List.of(item)));
        given(referenceLinkReader.existsByCanonicalUrl("https://n.news.naver.com/article/001/1")).willReturn(false);
        given(referenceLinkReader.countByKeywordAndProductIdOnDate("갤럭시북", 10L, LocalDate.of(2026, 6, 21)))
            .willReturn(0L);
        given(draftGenerator.generate(any(LaunchNewsPostDraftCommand.class))).willReturn(Optional.of(draft));
        given(postWriter.write(any(PostWriteCommand.class))).willReturn(42L);

        service.postLaunchNews(request(3));

        ArgumentCaptor<PostWriteCommand> postCommand = ArgumentCaptor.forClass(PostWriteCommand.class);
        verify(postWriter).write(postCommand.capture());
        assertThat(postCommand.getValue().boardCategory()).isEqualTo(expected);
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

    private NewsSourceItem newsItem(String title, String description) {
        return new NewsSourceItem(
            "갤럭시북 " + title + " launch",
            "갤럭시북 " + description,
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
}
