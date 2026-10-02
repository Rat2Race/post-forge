package dev.iamrat.board.like.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.iamrat.board.comment.application.CommentStore;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CommentLikeServiceTest {

    @Mock
    private CommentLikeStore commentLikeStore;

    @Mock
    private CommentStore commentStore;

    @InjectMocks
    private CommentLikeService commentLikeService;

    @Test
    @DisplayName("좋아요 행을 새로 넣으면 카운터를 1 올리고 현재 수를 돌려준다")
    void like_whenInserted_increasesCounter() {
        given(commentLikeStore.insertIfAbsent(2L, 2L)).willReturn(true);
        given(commentLikeStore.countByCommentId(2L)).willReturn(5L);

        LikeResult response = commentLikeService.like(2L, 2L);

        assertThat(response).isEqualTo(new LikeResult(true, 5L));
        verify(commentStore).addLikeCount(2L, 1L);
    }

    @Test
    @DisplayName("이미 좋아요한 상태면 카운터를 건드리지 않고 현재 수를 돌려준다")
    void like_whenAlreadyLiked_keepsCounter() {
        given(commentLikeStore.insertIfAbsent(1L, 1L)).willReturn(false);
        given(commentLikeStore.countByCommentId(1L)).willReturn(4L);

        LikeResult response = commentLikeService.like(1L, 1L);

        assertThat(response).isEqualTo(new LikeResult(true, 4L));
        verify(commentStore, never()).addLikeCount(anyLong(), anyLong());
    }

    @Test
    @DisplayName("좋아요를 지우면 카운터를 1 내리고 false를 돌려준다")
    void unlike_whenDeleted_decreasesCounter() {
        given(commentLikeStore.deleteByCommentIdAndAccountId(9L, 9L)).willReturn(1L);
        given(commentLikeStore.countByCommentId(9L)).willReturn(2L);

        LikeResult response = commentLikeService.unlike(9L, 9L);

        assertThat(response).isEqualTo(new LikeResult(false, 2L));
        verify(commentStore).addLikeCount(9L, -1L);
    }

    @Test
    @DisplayName("이미 취소한 좋아요를 다시 취소하면 카운터를 건드리지 않는다")
    void unlike_whenNotLiked_keepsCounter() {
        given(commentLikeStore.deleteByCommentIdAndAccountId(9L, 9L)).willReturn(0L);
        given(commentLikeStore.countByCommentId(9L)).willReturn(2L);

        LikeResult response = commentLikeService.unlike(9L, 9L);

        assertThat(response).isEqualTo(new LikeResult(false, 2L));
        verify(commentStore, never()).addLikeCount(anyLong(), anyLong());
    }

    @Test
    @DisplayName("댓글 좋아요 정보 조회 시 DB 기준 상태를 반환한다")
    void getLikeCounts_returnsCountMap() {
        List<Long> commentIds = List.of(5L, 6L);
        List<Object[]> rows = Collections.singletonList(new Object[]{5L, 8L});

        given(commentLikeStore.countByCommentIds(commentIds)).willReturn(rows);

        Map<Long, Long> result = commentLikeService.getLikeCounts(commentIds);

        assertThat(result).containsEntry(5L, 8L)
                .containsEntry(6L, 0L);
    }
}
