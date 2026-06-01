package dev.iamrat.board.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.like.domain.CommentLike;
import dev.iamrat.board.like.domain.PostLike;
import dev.iamrat.board.post.domain.Post;
import jakarta.persistence.PersistenceException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

@Tag("persistence")
@DataJpaTest
@ActiveProfiles("test")
@Import(BoardConstraintTest.JpaAuditingTestConfig.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class BoardConstraintTest {

    private final TestEntityManager entityManager;

    BoardConstraintTest(TestEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    @DisplayName("post.accountId는 필수이다")
    void postAccountIdMustNotBeNull() {
        Post post = post(null, "writer");

        assertThatThrownBy(() -> entityManager.persistAndFlush(post))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("post.nickname은 필수이다")
    void postNicknameMustNotBeNull() {
        Post post = post(1L, null);

        assertThatThrownBy(() -> entityManager.persistAndFlush(post))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("comment.accountId는 필수이다")
    void commentAccountIdMustNotBeNull() {
        Post post = entityManager.persistFlushFind(post(1L, "writer"));
        Comment comment = comment(post, null, "commenter");

        assertThatThrownBy(() -> entityManager.persistAndFlush(comment))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("comment.nickname은 필수이다")
    void commentNicknameMustNotBeNull() {
        Post post = entityManager.persistFlushFind(post(1L, "writer"));
        Comment comment = comment(post, 2L, null);

        assertThatThrownBy(() -> entityManager.persistAndFlush(comment))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("한 계정은 같은 게시글에 좋아요를 한 번만 누를 수 있다")
    void postLikeMustBeUniqueByPostAndAccount() {
        Post post = entityManager.persistFlushFind(post(1L, "writer"));
        entityManager.persistAndFlush(PostLike.of(post, 2L));

        assertThatThrownBy(() -> entityManager.persistAndFlush(PostLike.of(post, 2L)))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("한 계정은 같은 댓글에 좋아요를 한 번만 누를 수 있다")
    void commentLikeMustBeUniqueByCommentAndAccount() {
        Post post = entityManager.persistFlushFind(post(1L, "writer"));
        Comment comment = entityManager.persistFlushFind(comment(post, 2L, "commenter"));
        entityManager.persistAndFlush(CommentLike.of(comment, 3L));

        assertThatThrownBy(() -> entityManager.persistAndFlush(CommentLike.of(comment, 3L)))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("post.title은 100자를 초과할 수 없다")
    void postTitleMustNotExceed100Characters() {
        Post post = postBuilder()
            .title(repeat("t", 101))
            .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(post))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("post.content는 10000자를 초과할 수 없다")
    void postContentMustNotExceed10000Characters() {
        Post post = postBuilder()
            .content(repeat("c", 10001))
            .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(post))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("post.summary는 500자를 초과할 수 없다")
    void postSummaryMustNotExceed500Characters() {
        Post post = postBuilder()
            .summary(repeat("s", 501))
            .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(post))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("post tag는 50자를 초과할 수 없다")
    void postTagMustNotExceed50Characters() {
        Post post = postBuilder()
            .tags(List.of(repeat("t", 51)))
            .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(post))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("post.nickname은 50자를 초과할 수 없다")
    void postNicknameMustNotExceed50Characters() {
        Post post = post(1L, repeat("n", 51));

        assertThatThrownBy(() -> entityManager.persistAndFlush(post))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("comment.content는 500자를 초과할 수 없다")
    void commentContentMustNotExceed500Characters() {
        Post post = entityManager.persistFlushFind(post(1L, "writer"));
        Comment comment = Comment.builder()
            .post(post)
            .content(repeat("c", 501))
            .accountId(2L)
            .nickname("commenter")
            .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(comment))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("comment.nickname은 50자를 초과할 수 없다")
    void commentNicknameMustNotExceed50Characters() {
        Post post = entityManager.persistFlushFind(post(1L, "writer"));
        Comment comment = comment(post, 2L, repeat("n", 51));

        assertThatThrownBy(() -> entityManager.persistAndFlush(comment))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    private static Post post(Long accountId, String nickname) {
        return postBuilder()
            .accountId(accountId)
            .nickname(nickname)
            .build();
    }

    private static Post.PostBuilder postBuilder() {
        return Post.builder()
            .title("title")
            .content("content")
            .accountId(1L)
            .nickname("writer");
    }

    private static Comment comment(Post post, Long accountId, String nickname) {
        return Comment.builder()
            .post(post)
            .content("content")
            .accountId(accountId)
            .nickname(nickname)
            .build();
    }

    private static String repeat(String value, int count) {
        return value.repeat(count);
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingTestConfig {
        @Bean
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("constraint-test");
        }
    }
}
