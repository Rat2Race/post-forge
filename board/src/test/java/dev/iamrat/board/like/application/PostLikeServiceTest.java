package dev.iamrat.board.like.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.iamrat.board.post.domain.PostRepository;
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
class PostLikeServiceTest {

    @Mock
    private PostLikeStore postLikeStore;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostLikeService postLikeService;

    @Test
    @DisplayName("좋아요 행을 새로 넣으면 카운터를 1 올리고 현재 수를 돌려준다")
    void like_whenInserted_increasesCounter() {
        given(postLikeStore.insertIfAbsent(2L, 2L)).willReturn(true);
        given(postLikeStore.countByPostId(2L)).willReturn(5L);

        LikeResult response = postLikeService.like(2L, 2L);

        assertThat(response).isEqualTo(new LikeResult(true, 5L));
        verify(postRepository).addLikeCount(2L, 1L);
    }

    @Test
    @DisplayName("이미 좋아요한 상태면 카운터를 건드리지 않고 현재 수를 돌려준다")
    void like_whenAlreadyLiked_keepsCounter() {
        given(postLikeStore.insertIfAbsent(1L, 1L)).willReturn(false);
        given(postLikeStore.countByPostId(1L)).willReturn(4L);

        LikeResult response = postLikeService.like(1L, 1L);

        assertThat(response).isEqualTo(new LikeResult(true, 4L));
        verify(postRepository, never()).addLikeCount(anyLong(), anyLong());
    }

    @Test
    @DisplayName("좋아요를 지우면 카운터를 1 내리고 false를 돌려준다")
    void unlike_whenDeleted_decreasesCounter() {
        given(postLikeStore.deleteByPostIdAndAccountId(9L, 9L)).willReturn(1L);
        given(postLikeStore.countByPostId(9L)).willReturn(2L);

        LikeResult response = postLikeService.unlike(9L, 9L);

        assertThat(response).isEqualTo(new LikeResult(false, 2L));
        verify(postRepository).addLikeCount(9L, -1L);
    }

    @Test
    @DisplayName("이미 취소한 좋아요를 다시 취소하면 카운터를 건드리지 않는다")
    void unlike_whenNotLiked_keepsCounter() {
        given(postLikeStore.deleteByPostIdAndAccountId(9L, 9L)).willReturn(0L);
        given(postLikeStore.countByPostId(9L)).willReturn(2L);

        LikeResult response = postLikeService.unlike(9L, 9L);

        assertThat(response).isEqualTo(new LikeResult(false, 2L));
        verify(postRepository, never()).addLikeCount(anyLong(), anyLong());
    }

    @Test
    @DisplayName("좋아요 정보 조회 시 사용자 좋아요 여부와 카운트를 DB 기준으로 반환한다")
    void getLikeInfo_returnsCurrentState() {
        Long postId = 3L;

        given(postLikeStore.countByPostId(postId)).willReturn(7L);
        given(postLikeStore.existsByPostIdAndAccountId(postId, 3L)).willReturn(true);

        LikeResult response = postLikeService.getLikeInfo(postId, 3L);

        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(7L);
    }

    @Test
    @DisplayName("좋아요 수 목록 조회 시 DB 집계 결과를 ID별 맵으로 변환한다")
    void getLikeCounts_returnsCountMap() {
        List<Long> postIds = List.of(10L, 20L, 30L);
        List<Object[]> rows = List.of(
                new Object[]{10L, 2L},
                new Object[]{30L, 4L}
        );

        given(postLikeStore.countByPostIds(postIds)).willReturn(rows);

        Map<Long, Long> result = postLikeService.getLikeCounts(postIds);

        assertThat(result).containsEntry(10L, 2L)
                .containsEntry(20L, 0L)
                .containsEntry(30L, 4L);
    }
}
