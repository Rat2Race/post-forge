package dev.iamrat.board.post.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.infrastructure.persistence.PostPersistenceAdapter;
import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.DailyDigestSourceItem;
import dev.iamrat.core.board.post.PostCategory;
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
    BoardDailyDigestSourceReaderTest.JpaAuditingTestConfig.class,
    PostPersistenceAdapter.class,
    BoardDailyDigestSourceReader.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class BoardDailyDigestSourceReaderTest {

    private static final LocalDate NEWS_DATE = LocalDate.of(2026, 8, 20);

    private final TestEntityManager entityManager;
    private final BoardDailyDigestSourceReader reader;

    BoardDailyDigestSourceReaderTest(TestEntityManager entityManager, BoardDailyDigestSourceReader reader) {
        this.entityManager = entityManager;
        this.reader = reader;
    }

    @Test
    @DisplayName("해당 날짜와 분야의 출시 뉴스만 반환한다")
    void findLaunchNews_returnsOnlyLaunchNewsOfDateAndCategory() {
        Long matched = savePost("갤럭시북 출시", "요약", PostCategory.PRODUCT_LAUNCH_NEWS, BoardCategory.DIGITAL,
            NEWS_DATE.atTime(9, 0));
        savePost("전날 뉴스", "요약", PostCategory.PRODUCT_LAUNCH_NEWS, BoardCategory.DIGITAL,
            NEWS_DATE.minusDays(1).atTime(23, 59));
        savePost("다음날 뉴스", "요약", PostCategory.PRODUCT_LAUNCH_NEWS, BoardCategory.DIGITAL,
            NEWS_DATE.plusDays(1).atStartOfDay());
        savePost("다른 분야 뉴스", "요약", PostCategory.PRODUCT_LAUNCH_NEWS, BoardCategory.APPLIANCE,
            NEWS_DATE.atTime(9, 0));
        savePost("일반 게시글", "요약", PostCategory.GENERAL, BoardCategory.DIGITAL,
            NEWS_DATE.atTime(9, 0));

        List<DailyDigestSourceItem> items = reader.findLaunchNews(BoardCategory.DIGITAL, NEWS_DATE);

        assertThat(items).containsExactly(new DailyDigestSourceItem("갤럭시북 출시", "요약"));
        assertThat(matched).isNotNull();
    }

    @Test
    @DisplayName("자정 경계는 시작 포함, 끝 제외로 판정한다")
    void findLaunchNews_startOfDayInclusiveEndExclusive() {
        savePost("자정 뉴스", "요약", PostCategory.PRODUCT_LAUNCH_NEWS, BoardCategory.DIGITAL,
            NEWS_DATE.atStartOfDay());

        assertThat(reader.findLaunchNews(BoardCategory.DIGITAL, NEWS_DATE)).hasSize(1);
        assertThat(reader.findLaunchNews(BoardCategory.DIGITAL, NEWS_DATE.minusDays(1))).isEmpty();
    }

    @Test
    @DisplayName("같은 분야·제목의 데일리 브리핑이 있으면 digestExists가 true다")
    void digestExists_matchesCategoryAndTitle() {
        savePost("[디지털] 데일리 브리핑 - 2026-08-20", "요약", PostCategory.DAILY_DIGEST, BoardCategory.DIGITAL,
            NEWS_DATE.plusDays(1).atTime(6, 0));

        assertThat(reader.digestExists(BoardCategory.DIGITAL, "[디지털] 데일리 브리핑 - 2026-08-20")).isTrue();
        assertThat(reader.digestExists(BoardCategory.DIGITAL, "[디지털] 데일리 브리핑 - 2026-08-21")).isFalse();
        assertThat(reader.digestExists(BoardCategory.APPLIANCE, "[디지털] 데일리 브리핑 - 2026-08-20")).isFalse();
    }

    @Test
    @DisplayName("같은 제목이라도 데일리 브리핑 카테고리가 아니면 digestExists가 false다")
    void digestExists_ignoresOtherPostCategories() {
        savePost("[디지털] 데일리 브리핑 - 2026-08-20", "요약", PostCategory.GENERAL, BoardCategory.DIGITAL,
            NEWS_DATE.atTime(9, 0));

        assertThat(reader.digestExists(BoardCategory.DIGITAL, "[디지털] 데일리 브리핑 - 2026-08-20")).isFalse();
    }

    private Long savePost(
        String title,
        String summary,
        PostCategory category,
        BoardCategory boardCategory,
        LocalDateTime createdAt
    ) {
        Post post = entityManager.persistFlushFind(Post.create(
            title,
            "내용 " + title,
            summary,
            List.of(),
            category,
            boardCategory,
            dev.iamrat.core.board.post.PostPublishOrigin.SYSTEM_BATCH,
            0L,
            "PostForge News Bot"
        ));
        entityManager.getEntityManager()
            .createNativeQuery("UPDATE posts SET created_at = :createdAt WHERE id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", post.getId())
            .executeUpdate();
        entityManager.clear();
        return post.getId();
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingTestConfig {
        @Bean
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("digest-test");
        }
    }
}
