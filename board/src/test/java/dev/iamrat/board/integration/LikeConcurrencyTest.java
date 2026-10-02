package dev.iamrat.board.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import dev.iamrat.board.comment.application.CommentStore;
import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.like.application.CommentLikeService;
import dev.iamrat.board.like.application.CommentLikeStore;
import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.board.like.application.PostLikeService;
import dev.iamrat.board.like.application.PostLikeStore;
import dev.iamrat.board.like.infrastructure.persistence.CommentLikePersistenceAdapter;
import dev.iamrat.board.like.infrastructure.persistence.PostLikePersistenceAdapter;
import dev.iamrat.board.post.application.PostStore;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.view.application.ViewCountService;
import dev.iamrat.core.account.AccountProfileManager;
import dev.iamrat.core.account.AccountProfileReader;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 좋아요의 중복 요청과 동시 요청을 실제 PostgreSQL에서 확인한다.
 * 테스트 트랜잭션으로 감싸지 않는다. 요청마다 커밋해야 동시 요청의 갱신 손실과 실패한 INSERT 뒤의 트랜잭션 상태가 드러난다.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Import(LikeConcurrencyTest.StaleExistsCheck.class)
class LikeConcurrencyTest {

    @Autowired
    private PostLikeService postLikeService;

    @Autowired
    private CommentLikeService commentLikeService;

    @Autowired
    private PostStore postStore;

    @Autowired
    private CommentStore commentStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ViewCountService viewCountService;

    @MockitoBean
    private AccountProfileReader accountProfileReader;

    @MockitoBean
    private AccountProfileManager accountProfileManager;

    private Post post;

    @BeforeEach
    void createPost() {
        post = postStore.save(Post.create("좋아요 동시성", "본문", null, 1L, "writer"));
    }

    @AfterEach
    void deletePost() {
        jdbcTemplate.update("DELETE FROM comment_like WHERE comment_id IN (SELECT id FROM comments WHERE post_id = ?)", post.getId());
        jdbcTemplate.update("DELETE FROM comments WHERE post_id = ?", post.getId());
        jdbcTemplate.update("DELETE FROM post_like WHERE post_id = ?", post.getId());
        jdbcTemplate.update("DELETE FROM posts WHERE id = ?", post.getId());
    }

    @Test
    @DisplayName("같은 계정의 좋아요 두 개가 함께 '아직 없음'을 봐도 늦은 쪽도 성공하고 좋아요는 하나다")
    void duplicatePostLikeAfterStaleCheckSucceeds() {
        postLikeService.like(post.getId(), 7L);

        LikeResult late = postLikeService.like(post.getId(), 7L);

        assertThat(late).isEqualTo(new LikeResult(true, 1L));
        assertThat(likeCountColumn("posts", post.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 계정의 댓글 좋아요 두 개가 함께 '아직 없음'을 봐도 늦은 쪽도 성공하고 좋아요는 하나다")
    void duplicateCommentLikeAfterStaleCheckSucceeds() {
        Long commentId = commentStore.save(Comment.create(post, null, "댓글", 2L, "commenter")).getId();
        commentLikeService.like(commentId, 7L);

        LikeResult late = commentLikeService.like(commentId, 7L);

        assertThat(late).isEqualTo(new LikeResult(true, 1L));
        assertThat(likeCountColumn("comments", commentId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("계정 20개가 동시에 좋아요를 눌러도 좋아요 수와 카운터가 모두 20이다")
    void concurrentLikesFromTwentyAccountsKeepCounter() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<LikeResult>> results = LongStream.rangeClosed(1, 20)
                .mapToObj(accountId -> pool.submit(() -> {
                    start.await();
                    return postLikeService.like(post.getId(), accountId);
                }))
                .toList();
            start.countDown();
            for (Future<LikeResult> result : results) {
                result.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(postLikeService.getLikeCounts(List.of(post.getId()))).containsEntry(post.getId(), 20L);
        assertThat(likeCountColumn("posts", post.getId())).isEqualTo(20L);
    }

    private long likeCountColumn(String table, Long id) {
        return jdbcTemplate.queryForObject("SELECT like_count FROM " + table + " WHERE id = ?", Long.class, id);
    }

    @TestConfiguration
    static class StaleExistsCheck {

        // 같은 계정의 요청 두 개가 함께 "아직 없음"을 본 순간을 고정한다: 이미 좋아요한 행이 있어도 없다고 답한다.
        @Bean
        @Primary
        PostLikeStore staleExistsPostLikeStore(PostLikePersistenceAdapter adapter) {
            PostLikeStore store = mock(PostLikeStore.class, AdditionalAnswers.delegatesTo(adapter));
            doReturn(false).when(store).existsByPostIdAndAccountId(anyLong(), anyLong());
            return store;
        }

        @Bean
        @Primary
        CommentLikeStore staleExistsCommentLikeStore(CommentLikePersistenceAdapter adapter) {
            CommentLikeStore store = mock(CommentLikeStore.class, AdditionalAnswers.delegatesTo(adapter));
            doReturn(false).when(store).existsByCommentIdAndAccountId(anyLong(), anyLong());
            return store;
        }
    }
}
