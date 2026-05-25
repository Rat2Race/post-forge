package dev.iamrat.board.like.application;

import dev.iamrat.board.like.domain.PostLike;
import dev.iamrat.board.post.application.PostLikeTargetService;
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
class PostLikeServiceTest {

    @Mock
    private PostLikeStore postLikeStore;

    @Mock
    private PostLikeTargetService postLikeTargetService;

    @InjectMocks
    private PostLikeService postLikeService;

    @Test
    @DisplayName("이미 좋아요 상태여도 like 요청은 저장하지 않고 true를 반환한다")
    void like_whenAlreadyLiked_returnsCurrentState() {
        Long postId = 1L;

        given(postLikeStore.existsByPostIdAndAccountId(postId, 1L)).willReturn(true);
        given(postLikeStore.countByPostId(postId)).willReturn(4L);

        LikeResponse response = postLikeService.like(postId, 1L);

        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(4L);
        verify(postLikeStore, never()).save(any(PostLike.class));
        verify(postLikeTargetService).updateLikeCount(postId, 4L);
    }

    @Test
    @DisplayName("좋아요 상태가 아니면 DB에 저장하고 true를 반환한다")
    void like_whenNotLiked_savesLike() {
        Long postId = 2L;
        Post postRef = Post.builder().id(postId).title("title").content("content").accountId(9L).build();

        given(postLikeStore.existsByPostIdAndAccountId(postId, 2L)).willReturn(false);
        given(postLikeTargetService.getReference(postId)).willReturn(postRef);
        given(postLikeStore.countByPostId(postId)).willReturn(5L);

        LikeResponse response = postLikeService.like(postId, 2L);

        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(5L);

        ArgumentCaptor<PostLike> likeCaptor = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeStore).save(likeCaptor.capture());
        assertThat(likeCaptor.getValue().getPost()).isEqualTo(postRef);
        assertThat(likeCaptor.getValue().getAccountId()).isEqualTo(2L);
        verify(postLikeTargetService).updateLikeCount(postId, 5L);
    }

    @Test
    @DisplayName("unlike 요청은 좋아요를 삭제하고 false를 반환한다")
    void unlike_removesLike() {
        Long postId = 9L;

        given(postLikeStore.deleteByPostIdAndAccountId(postId, 9L)).willReturn(1L);
        given(postLikeStore.countByPostId(postId)).willReturn(2L);

        LikeResponse response = postLikeService.unlike(postId, 9L);

        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(2L);
        verify(postLikeTargetService).updateLikeCount(postId, 2L);
    }

    @Test
    @DisplayName("좋아요 정보 조회 시 사용자 좋아요 여부와 카운트를 DB 기준으로 반환한다")
    void getLikeInfo_returnsCurrentState() {
        Long postId = 3L;

        given(postLikeStore.countByPostId(postId)).willReturn(7L);
        given(postLikeStore.existsByPostIdAndAccountId(postId, 3L)).willReturn(true);

        LikeResponse response = postLikeService.getLikeInfo(postId, 3L);

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

    @Test
    @DisplayName("사용자 좋아요 목록 조회 시 DB 결과를 그대로 반환한다")
    void getLikedPostIds_returnsLikedIds() {
        List<Long> postIds = List.of(10L, 20L);

        given(postLikeStore.findLikedPostIdsByAccountIdAndPostIds(1L, postIds)).willReturn(Set.of(20L));

        Set<Long> result = postLikeService.getLikedPostIds(postIds, 1L);

        assertThat(result).containsExactly(20L);
    }

}
