package dev.iamrat.board.post.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

@Tag("persistence")
@DataJpaTest
@ActiveProfiles("test")
@Import({PostPersistenceAdapter.class, PostPersistenceAdapterFilterTest.JpaAuditingTestConfig.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PostPersistenceAdapterFilterTest {

    private final PostPersistenceAdapter adapter;
    private final TestEntityManager entityManager;

    PostPersistenceAdapterFilterTest(PostPersistenceAdapter adapter, TestEntityManager entityManager) {
        this.adapter = adapter;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        entityManager.persistAndFlush(post(
            "Galaxy Book 출시", "신제품 소개", PostCategory.PRODUCT_LAUNCH_NEWS,
            BoardCategory.DIGITAL, PostPublishOrigin.SYSTEM_BATCH));
        entityManager.persistAndFlush(post(
            "일반 글", "galaxy book 사용 후기", PostCategory.GENERAL,
            BoardCategory.GENERAL, PostPublishOrigin.USER));
        entityManager.persistAndFlush(post(
            "무관한 글", "관계 없는 본문", PostCategory.DAILY_DIGEST,
            BoardCategory.LIVING, PostPublishOrigin.ADMIN_BACKFILL));
    }

    @Test
    @DisplayName("keyword는 제목과 본문을 대소문자 구분 없이 함께 검색한다")
    void filtersByKeywordOnTitleOrContent() {
        assertThat(adapter.findByFilters("Galaxy", null, null, null, PageRequest.of(0, 10)))
            .extracting(Post::getTitle)
            .containsExactlyInAnyOrder("Galaxy Book 출시", "일반 글");
    }

    @Test
    @DisplayName("category가 있으면 해당 category만 조회한다")
    void filtersByCategory() {
        assertThat(adapter.findByFilters(null, PostCategory.PRODUCT_LAUNCH_NEWS, null, null, PageRequest.of(0, 10)))
            .extracting(Post::getTitle)
            .containsExactly("Galaxy Book 출시");
    }

    @Test
    @DisplayName("boardCategory가 있으면 해당 boardCategory만 조회한다")
    void filtersByBoardCategory() {
        assertThat(adapter.findByFilters(null, null, BoardCategory.LIVING, null, PageRequest.of(0, 10)))
            .extracting(Post::getTitle)
            .containsExactly("무관한 글");
    }

    @Test
    @DisplayName("publishOrigin이 있으면 해당 publishOrigin만 조회한다")
    void filtersByPublishOrigin() {
        assertThat(adapter.findByFilters(null, null, null, PostPublishOrigin.SYSTEM_BATCH, PageRequest.of(0, 10)))
            .extracting(Post::getTitle)
            .containsExactly("Galaxy Book 출시");
    }

    @Test
    @DisplayName("빈 keyword와 null 필터만 있으면 조건 없이 전체를 조회한다")
    void returnsAllWhenNoFilterApplied() {
        assertThat(adapter.findByFilters("   ", null, null, null, PageRequest.of(0, 10)))
            .extracting(Post::getTitle)
            .containsExactlyInAnyOrder("Galaxy Book 출시", "일반 글", "무관한 글");
    }

    private static Post post(
        String title,
        String content,
        PostCategory category,
        BoardCategory boardCategory,
        PostPublishOrigin publishOrigin
    ) {
        return Post.create(title, content, null, null, category, boardCategory, publishOrigin, 1L, "writer");
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingTestConfig {
        @Bean
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("filter-test");
        }
    }
}
