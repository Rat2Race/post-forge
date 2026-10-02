package dev.iamrat.board.post.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostReferenceLink;
import dev.iamrat.board.post.domain.PostType;
import dev.iamrat.board.post.infrastructure.persistence.PostPersistenceAdapter;
import dev.iamrat.board.post.infrastructure.persistence.PostReferenceLinkPersistenceAdapter;
import dev.iamrat.core.board.post.DailyDigestDraft;
import dev.iamrat.core.board.post.DailyDigestSourceItem;
import dev.iamrat.core.board.post.LaunchNewsPost;
import dev.iamrat.core.board.post.LaunchNewsPostDraft;
import dev.iamrat.core.board.post.NewsSection;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

@Tag("persistence")
@DataJpaTest
@ActiveProfiles("test")
@Import({
    BoardNewsPostPortTest.JpaAuditingTestConfig.class,
    PostPersistenceAdapter.class,
    PostReferenceLinkPersistenceAdapter.class,
    BoardNewsPostPort.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class BoardNewsPostPortTest {

    private static final LocalDate NEWS_DATE = LocalDate.of(2026, 8, 20);

    private final TestEntityManager entityManager;
    private final BoardNewsPostPort port;

    BoardNewsPostPortTest(TestEntityManager entityManager, BoardNewsPostPort port) {
        this.entityManager = entityManager;
        this.port = port;
    }

    @Test
    @DisplayName("출시 뉴스는 봇 작성자의 글과 출처 링크를 함께 저장한다")
    void publishLaunchNews_savesBotPostWithReferenceLink() {
        Long postId = port.publishLaunchNews(launchNews("https://news.example/a", PostPublishOrigin.ADMIN_BACKFILL));
        entityManager.flush();
        entityManager.clear();

        Post post = entityManager.find(Post.class, postId);
        assertThat(post.getTitle()).isEqualTo("갤럭시북 신제품 출시");
        assertThat(post.getSummary()).isEqualTo("요약");
        assertThat(post.getTags()).containsExactly("갤럭시북");
        assertThat(post.getCategory()).isEqualTo(PostType.PRODUCT_LAUNCH_NEWS);
        assertThat(post.getBoardCategory()).isEqualTo(NewsSection.TECHNOLOGY);
        assertThat(post.getPublishOrigin()).isEqualTo(PostPublishOrigin.ADMIN_BACKFILL);
        assertThat(post.getAccountId()).isEqualTo(0L);
        assertThat(post.getNickname()).isEqualTo("PostForge News Bot");

        PostReferenceLink link = entityManager.getEntityManager()
            .createQuery("SELECT l FROM PostReferenceLink l WHERE l.post.id = :id", PostReferenceLink.class)
            .setParameter("id", postId)
            .getSingleResult();
        assertThat(link.getKeyword()).isEqualTo("갤럭시북");
        assertThat(link.getCanonicalUrl()).isEqualTo("https://news.example/a");
        assertThat(link.getOriginalUrl()).isEqualTo("https://news.example/a?utm_source=x");
        assertThat(link.getSourceName()).isEqualTo("news.example");
        assertThat(link.getTitleSnapshot()).isEqualTo("원문 제목");
        assertThat(link.getPublishedAt()).isEqualTo(NEWS_DATE.atTime(9, 0));
    }

    @Test
    @DisplayName("게시한 출시 뉴스는 URL 중복과 키워드별 일일 건수에 잡힌다")
    void publishLaunchNews_isVisibleToDuplicateAndDailyCapChecks() {
        assertThat(port.isPublished("https://news.example/a")).isFalse();

        port.publishLaunchNews(launchNews("https://news.example/a", PostPublishOrigin.SYSTEM_BATCH));

        assertThat(port.isPublished("https://news.example/a")).isTrue();
        assertThat(port.countPublished("갤럭시북", NEWS_DATE)).isEqualTo(1);
        assertThat(port.countPublished("갤럭시북", NEWS_DATE.plusDays(1))).isZero();
    }

    @Test
    @DisplayName("해당 날짜와 분야의 출시 뉴스만 반환한다")
    void findLaunchNews_returnsOnlyLaunchNewsOfDateAndSection() {
        savePost("갤럭시북 출시", PostType.PRODUCT_LAUNCH_NEWS, NewsSection.TECHNOLOGY, NEWS_DATE.atTime(9, 0));
        savePost("전날 뉴스", PostType.PRODUCT_LAUNCH_NEWS, NewsSection.TECHNOLOGY,
            NEWS_DATE.minusDays(1).atTime(23, 59));
        savePost("다음날 뉴스", PostType.PRODUCT_LAUNCH_NEWS, NewsSection.TECHNOLOGY,
            NEWS_DATE.plusDays(1).atStartOfDay());
        savePost("다른 분야 뉴스", PostType.PRODUCT_LAUNCH_NEWS, NewsSection.BUSINESS, NEWS_DATE.atTime(9, 0));
        savePost("일반 게시글", PostType.GENERAL, NewsSection.TECHNOLOGY, NEWS_DATE.atTime(9, 0));

        List<DailyDigestSourceItem> items = port.findLaunchNews(NewsSection.TECHNOLOGY, NEWS_DATE);

        assertThat(items).containsExactly(new DailyDigestSourceItem("갤럭시북 출시", "요약"));
    }

    @Test
    @DisplayName("자정 경계는 시작 포함, 끝 제외로 판정한다")
    void findLaunchNews_startOfDayInclusiveEndExclusive() {
        savePost("자정 뉴스", PostType.PRODUCT_LAUNCH_NEWS, NewsSection.TECHNOLOGY, NEWS_DATE.atStartOfDay());

        assertThat(port.findLaunchNews(NewsSection.TECHNOLOGY, NEWS_DATE)).hasSize(1);
        assertThat(port.findLaunchNews(NewsSection.TECHNOLOGY, NEWS_DATE.minusDays(1))).isEmpty();
    }

    @Test
    @DisplayName("데일리 브리핑은 분야·날짜 제목과 공백을 접은 요약으로 봇이 게시한다")
    void publishDailyDigest_savesBotDigestWithTitleAndSummary() {
        Long postId = port.publishDailyDigest(NewsSection.TECHNOLOGY, NEWS_DATE,
            new DailyDigestDraft("디지털  브리핑\n본문", List.of("technology", "daily-digest")));
        entityManager.flush();
        entityManager.clear();

        Post post = entityManager.find(Post.class, postId);
        assertThat(post.getTitle()).isEqualTo("[과학/기술] 데일리 브리핑 - 2026-08-20");
        assertThat(post.getContent()).isEqualTo("디지털  브리핑\n본문");
        assertThat(post.getSummary()).isEqualTo("디지털 브리핑 본문");
        assertThat(post.getTags()).containsExactly("technology", "daily-digest");
        assertThat(post.getCategory()).isEqualTo(PostType.DAILY_DIGEST);
        assertThat(post.getBoardCategory()).isEqualTo(NewsSection.TECHNOLOGY);
        assertThat(post.getPublishOrigin()).isEqualTo(PostPublishOrigin.SYSTEM_BATCH);
        assertThat(post.getAccountId()).isEqualTo(0L);
        assertThat(post.getNickname()).isEqualTo("PostForge News Bot");
    }

    @Test
    @DisplayName("데일리 요약은 500자로 자르되 surrogate pair를 나누지 않는다")
    void publishDailyDigest_abbreviatesSummaryWithoutSplittingSurrogatePair() {
        Long longId = port.publishDailyDigest(NewsSection.TECHNOLOGY, NEWS_DATE,
            new DailyDigestDraft("가".repeat(700), List.of()));
        Long emojiId = port.publishDailyDigest(NewsSection.BUSINESS, NEWS_DATE,
            new DailyDigestDraft("가".repeat(499) + "😀나", List.of()));
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Post.class, longId).getSummary()).isEqualTo("가".repeat(500));
        assertThat(entityManager.find(Post.class, emojiId).getSummary()).isEqualTo("가".repeat(499));
    }

    @Test
    @DisplayName("게시한 데일리 브리핑은 같은 분야·날짜에서만 digestExists가 true다")
    void digestExists_matchesPublishedSectionAndDate() {
        port.publishDailyDigest(NewsSection.TECHNOLOGY, NEWS_DATE, new DailyDigestDraft("본문", List.of()));

        assertThat(port.digestExists(NewsSection.TECHNOLOGY, NEWS_DATE)).isTrue();
        assertThat(port.digestExists(NewsSection.TECHNOLOGY, NEWS_DATE.plusDays(1))).isFalse();
        assertThat(port.digestExists(NewsSection.BUSINESS, NEWS_DATE)).isFalse();
    }

    @Test
    @DisplayName("같은 제목이라도 데일리 브리핑 종류가 아니면 digestExists가 false다")
    void digestExists_ignoresOtherPostTypes() {
        savePost("[과학/기술] 데일리 브리핑 - 2026-08-20", PostType.GENERAL, NewsSection.TECHNOLOGY,
            NEWS_DATE.atTime(9, 0));

        assertThat(port.digestExists(NewsSection.TECHNOLOGY, NEWS_DATE)).isFalse();
    }

    private LaunchNewsPost launchNews(String canonicalUrl, PostPublishOrigin publishOrigin) {
        return new LaunchNewsPost(
            new LaunchNewsPostDraft("갤럭시북 신제품 출시", "본문", "요약", List.of("갤럭시북")),
            NewsSection.TECHNOLOGY,
            publishOrigin,
            "갤럭시북",
            "원문 제목",
            canonicalUrl,
            canonicalUrl + "?utm_source=x",
            "news.example",
            NEWS_DATE.atTime(9, 0)
        );
    }

    private void savePost(String title, PostType type, NewsSection section, LocalDateTime createdAt) {
        Post post = entityManager.persistFlushFind(Post.create(
            title, "내용 " + title, "요약", List.of(), type, section, PostPublishOrigin.SYSTEM_BATCH,
            0L, "PostForge News Bot"
        ));
        entityManager.getEntityManager()
            .createNativeQuery("UPDATE posts SET created_at = :createdAt WHERE id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", post.getId())
            .executeUpdate();
        entityManager.clear();
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingTestConfig {
        @Bean
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("news-post-test");
        }
    }
}
