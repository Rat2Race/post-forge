package dev.iamrat.board.post.domain;

import static org.assertj.core.api.Assertions.assertThat;

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
@Import(PostRepositoryKeywordTest.JpaAuditingTestConfig.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PostRepositoryKeywordTest {

    private final PostRepository postRepository;
    private final TestEntityManager entityManager;

    PostRepositoryKeywordTest(PostRepository postRepository, TestEntityManager entityManager) {
        this.postRepository = postRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        entityManager.persistAndFlush(post("Galaxy Book 리뷰", "가벼운 노트북"));
        entityManager.persistAndFlush(post("일반 글", "galaxy book 사용 후기"));
        entityManager.persistAndFlush(post("무관한 글", "관계 없는 본문"));
    }

    @Test
    @DisplayName("keyword는 제목과 본문을 대소문자 구분 없이 함께 검색한다")
    void filtersByKeywordOnTitleOrContent() {
        assertThat(postRepository.findByKeyword("Galaxy", PageRequest.of(0, 10)))
            .extracting(Post::getTitle)
            .containsExactlyInAnyOrder("Galaxy Book 리뷰", "일반 글");
    }

    @Test
    @DisplayName("빈 keyword면 조건 없이 전체를 조회한다")
    void returnsAllWhenKeywordIsBlank() {
        assertThat(postRepository.findByKeyword("   ", PageRequest.of(0, 10)))
            .extracting(Post::getTitle)
            .containsExactlyInAnyOrder("Galaxy Book 리뷰", "일반 글", "무관한 글");
    }

    private static Post post(String title, String content) {
        return Post.create(title, content, null, 1L, "writer");
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
