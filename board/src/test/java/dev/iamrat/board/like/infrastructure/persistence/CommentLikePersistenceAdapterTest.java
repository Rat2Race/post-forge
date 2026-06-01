package dev.iamrat.board.like.infrastructure.persistence;

import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.like.domain.CommentLike;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentLikePersistenceAdapterTest {

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @InjectMocks
    private CommentLikePersistenceAdapter commentLikePersistenceAdapter;

    @Test
    @DisplayName("existsByCommentIdAndAccountId는 Spring Data repository에 위임한다")
    void existsByCommentIdAndAccountId_delegatesToRepository() {
        given(commentLikeRepository.existsByComment_IdAndAccountId(1L, 2L)).willReturn(true);

        boolean result = commentLikePersistenceAdapter.existsByCommentIdAndAccountId(1L, 2L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("save는 Spring Data repository에 위임한다")
    void save_delegatesToRepository() {
        CommentLike commentLike = CommentLike.of(comment(1L), 2L);
        given(commentLikeRepository.save(commentLike)).willReturn(commentLike);

        CommentLike result = commentLikePersistenceAdapter.save(commentLike);

        assertThat(result).isSameAs(commentLike);
    }

    @Test
    @DisplayName("count와 delete는 Spring Data repository에 위임한다")
    void countAndDelete_delegateToRepository() {
        given(commentLikeRepository.countByComment_Id(1L)).willReturn(3L);
        given(commentLikeRepository.deleteByComment_IdAndAccountId(1L, 2L)).willReturn(1L);

        assertThat(commentLikePersistenceAdapter.countByCommentId(1L)).isEqualTo(3L);
        assertThat(commentLikePersistenceAdapter.deleteByCommentIdAndAccountId(1L, 2L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("bulk 조회는 Spring Data repository에 위임한다")
    void bulkQueries_delegateToRepository() {
        List<Object[]> rows = List.<Object[]>of(new Object[]{1L, 2L});
        given(commentLikeRepository.countByCommentIds(List.of(1L))).willReturn(rows);
        given(commentLikeRepository.findLikedCommentIdsByAccountIdAndCommentIds(2L, List.of(1L)))
            .willReturn(Set.of(1L));

        assertThat(commentLikePersistenceAdapter.countByCommentIds(List.of(1L))).isSameAs(rows);
        assertThat(commentLikePersistenceAdapter.findLikedCommentIdsByAccountIdAndCommentIds(2L, List.of(1L)))
            .containsExactly(1L);
    }

    @Test
    @DisplayName("삭제 갱신 경로는 repository를 호출한다")
    void deleteByCommentIdAndAccountId_invokesRepository() {
        commentLikePersistenceAdapter.deleteByCommentIdAndAccountId(1L, 2L);

        verify(commentLikeRepository).deleteByComment_IdAndAccountId(1L, 2L);
    }

    private Comment comment(Long commentId) {
        return Comment.builder()
            .id(commentId)
            .post(Post.builder().id(1L).title("title").content("content").accountId(1L).build())
            .content("comment")
            .accountId(3L)
            .nickname("writer")
            .build();
    }
}
