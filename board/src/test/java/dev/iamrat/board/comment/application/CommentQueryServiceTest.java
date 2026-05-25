package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.comment.dto.CommentDetailResponse;
import dev.iamrat.board.like.application.CommentLikeService;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceTest {

    @Mock
    private CommentStore commentStore;

    @Mock
    private CommentLikeService commentLikeService;

    @InjectMocks
    private CommentQueryService commentQueryService;

    @Test
    @DisplayName("댓글 목록에 좋아요 수와 내 좋아요 여부를 합성한다")
    void getCommentsByPost_combinesLikeState() {
        PageRequest pageable = PageRequest.of(0, 10);
        Comment comment = comment(10L);
        given(commentStore.findByPostId(1L, pageable))
            .willReturn(new PageImpl<>(List.of(comment), pageable, 1));
        given(commentLikeService.getLikeCounts(List.of(10L))).willReturn(Map.of(10L, 3L));
        given(commentLikeService.getLikedCommentIds(List.of(10L), 2L)).willReturn(Set.of(10L));

        Page<CommentDetailResponse> result = commentQueryService.getCommentsByPost(1L, pageable, 2L);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(10L);
        assertThat(result.getContent().get(0).likeCount()).isEqualTo(3L);
        assertThat(result.getContent().get(0).isLiked()).isTrue();
    }

    @Test
    @DisplayName("게시글별 댓글 수 집계를 맵으로 변환한다")
    void getCommentCounts_returnsCountMap() {
        given(commentStore.countByPostIds(List.of(1L, 2L))).willReturn(List.of(
            new Object[]{1L, 2L},
            new Object[]{2L, 4L}
        ));

        Map<Long, Integer> result = commentQueryService.getCommentCounts(List.of(1L, 2L));

        assertThat(result).containsEntry(1L, 2)
            .containsEntry(2L, 4);
    }

    private Comment comment(Long commentId) {
        return Comment.builder()
            .id(commentId)
            .post(Post.builder().id(1L).title("title").content("content").accountId(1L).build())
            .content("comment")
            .accountId(2L)
            .nickname("writer")
            .build();
    }
}
