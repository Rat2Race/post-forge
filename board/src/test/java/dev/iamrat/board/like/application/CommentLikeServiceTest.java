package dev.iamrat.board.like.application;

import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.comment.application.CommentLikeTargetService;
import dev.iamrat.board.like.domain.CommentLike;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CommentLikeServiceTest {

    @Mock
    private CommentLikeStore commentLikeStore;

    @Mock
    private CommentLikeTargetService commentLikeTargetService;

    @InjectMocks
    private CommentLikeService commentLikeService;

    @Test
    @DisplayName("이미 좋아요 상태여도 like 요청은 현재 상태를 반환한다")
    void like_whenAlreadyLiked_returnsCurrentState() {
        Long commentId = 1L;

        given(commentLikeStore.existsByCommentIdAndAccountId(commentId, 1L)).willReturn(true);
        given(commentLikeStore.countByCommentId(commentId)).willReturn(2L);

        LikeResponse response = commentLikeService.like(commentId, 1L);

        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(2L);
        verify(commentLikeStore, never()).save(any(CommentLike.class));
        verify(commentLikeTargetService).updateLikeCount(commentId, 2L);
    }

    @Test
    @DisplayName("좋아요 상태가 아니면 댓글 좋아요를 저장하고 true를 반환한다")
    void like_whenNotLiked_savesLike() {
        Long commentId = 2L;
        Comment commentRef = Comment.builder()
                .id(commentId)
                .post(Post.builder().id(1L).title("title").content("content").accountId(9L).build())
                .content("comment")
                .accountId(2L)
                .build();

        given(commentLikeStore.existsByCommentIdAndAccountId(commentId, 2L)).willReturn(false);
        given(commentLikeTargetService.getReference(commentId)).willReturn(commentRef);
        given(commentLikeStore.countByCommentId(commentId)).willReturn(3L);

        LikeResponse response = commentLikeService.like(commentId, 2L);

        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(3L);

        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikeStore).save(likeCaptor.capture());
        assertThat(likeCaptor.getValue().getComment()).isEqualTo(commentRef);
        assertThat(likeCaptor.getValue().getAccountId()).isEqualTo(2L);
        verify(commentLikeTargetService).updateLikeCount(commentId, 3L);
    }

    @Test
    @DisplayName("unlike 요청은 댓글 좋아요를 삭제하고 false를 반환한다")
    void unlike_removesLike() {
        Long commentId = 4L;

        given(commentLikeStore.deleteByCommentIdAndAccountId(commentId, 4L)).willReturn(1L);
        given(commentLikeStore.countByCommentId(commentId)).willReturn(1L);

        LikeResponse response = commentLikeService.unlike(commentId, 4L);

        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(1L);
        verify(commentLikeTargetService).updateLikeCount(commentId, 1L);
    }

    @Test
    @DisplayName("댓글 좋아요 정보 조회 시 DB 기준 상태를 반환한다")
    void getLikeCounts_returnsCountMap() {
        List<Long> commentIds = List.of(5L, 6L);
        List<Object[]> rows = java.util.Collections.singletonList(new Object[]{5L, 8L});

        given(commentLikeStore.countByCommentIds(commentIds)).willReturn(rows);

        Map<Long, Long> result = commentLikeService.getLikeCounts(commentIds);

        assertThat(result).containsEntry(5L, 8L)
                .containsEntry(6L, 0L);
    }

    @Test
    @DisplayName("사용자 댓글 좋아요 목록 조회 시 DB 결과를 반환한다")
    void getLikedCommentIds_returnsLikedIds() {
        List<Long> commentIds = List.of(7L, 8L);

        given(commentLikeStore.findLikedCommentIdsByAccountIdAndCommentIds(1L, commentIds))
                .willReturn(Set.of(8L));

        Set<Long> result = commentLikeService.getLikedCommentIds(commentIds, 1L);

        assertThat(result).containsExactly(8L);
    }

}
