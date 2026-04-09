package dev.iamrat.like.post.service;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.like.dto.LikeResponse;
import dev.iamrat.like.post.entity.PostLike;
import dev.iamrat.like.post.repository.PostLikeRepository;
import dev.iamrat.post.entity.Post;
import dev.iamrat.post.repository.PostRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostLikeService postLikeService;

    @Test
    @DisplayName("이미 좋아요 상태여도 like 요청은 저장하지 않고 true를 반환한다")
    void like_whenAlreadyLiked_returnsCurrentState() {
        Long postId = 1L;

        given(postLikeRepository.existsByPost_IdAndUserId(postId, "user1")).willReturn(true);
        given(postLikeRepository.countByPost_Id(postId)).willReturn(4L);

        LikeResponse response = postLikeService.like(postId, "user1");

        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(4L);
        verify(postLikeRepository, never()).save(any(PostLike.class));
        verify(postRepository).updateLikeCount(postId, 4L);
    }

    @Test
    @DisplayName("좋아요 상태가 아니면 DB에 저장하고 true를 반환한다")
    void like_whenNotLiked_savesLike() {
        Long postId = 2L;
        Post postRef = Post.builder().id(postId).title("title").content("content").userId("author").build();

        given(postLikeRepository.existsByPost_IdAndUserId(postId, "user2")).willReturn(false);
        given(postRepository.getReferenceById(postId)).willReturn(postRef);
        given(postLikeRepository.countByPost_Id(postId)).willReturn(5L);

        LikeResponse response = postLikeService.like(postId, "user2");

        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(5L);

        ArgumentCaptor<PostLike> likeCaptor = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeRepository).save(likeCaptor.capture());
        assertThat(likeCaptor.getValue().getPost()).isEqualTo(postRef);
        assertThat(likeCaptor.getValue().getUserId()).isEqualTo("user2");
        verify(postRepository).updateLikeCount(postId, 5L);
    }

    @Test
    @DisplayName("unlike 요청은 좋아요를 삭제하고 false를 반환한다")
    void unlike_removesLike() {
        Long postId = 9L;

        given(postLikeRepository.deleteByPost_IdAndUserId(postId, "user9")).willReturn(1L);
        given(postLikeRepository.countByPost_Id(postId)).willReturn(2L);

        LikeResponse response = postLikeService.unlike(postId, "user9");

        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(2L);
        verify(postRepository).updateLikeCount(postId, 2L);
    }

    @Test
    @DisplayName("좋아요 정보 조회 시 사용자 좋아요 여부와 카운트를 DB 기준으로 반환한다")
    void getLikeInfo_returnsCurrentState() {
        Long postId = 3L;

        given(postLikeRepository.countByPost_Id(postId)).willReturn(7L);
        given(postLikeRepository.existsByPost_IdAndUserId(postId, "user3")).willReturn(true);

        LikeResponse response = postLikeService.getLikeInfo(postId, "user3");

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

        given(postLikeRepository.countByPostIds(postIds)).willReturn(rows);

        Map<Long, Long> result = postLikeService.getLikeCounts(postIds);

        assertThat(result).containsEntry(10L, 2L)
                .containsEntry(20L, 0L)
                .containsEntry(30L, 4L);
    }

    @Test
    @DisplayName("사용자 좋아요 목록 조회 시 DB 결과를 그대로 반환한다")
    void getLikedPostIds_returnsLikedIds() {
        List<Long> postIds = List.of(10L, 20L);

        given(postLikeRepository.findLikedPostIdsByUserIdAndPostIds("user1", postIds)).willReturn(Set.of(20L));

        Set<Long> result = postLikeService.getLikedPostIds(postIds, "user1");

        assertThat(result).containsExactly(20L);
    }

    @Test
    @DisplayName("postIds가 null이면 INVALID_INPUT 예외를 던진다")
    void getLikeCounts_whenPostIdsNull_throwsInvalidInput() {
        assertThatThrownBy(() -> postLikeService.getLikeCounts(null))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
