package dev.iamrat.board.post.application;

import dev.iamrat.board.comment.application.CommentQueryService;
import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.board.like.application.PostLikeService;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.view.application.ViewCountService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    @Mock
    private PostStore postStore;

    @Mock
    private PostReader postReader;

    @Mock
    private PostLikeService postLikeService;

    @Mock
    private CommentQueryService commentQueryService;

    @Mock
    private ViewCountService viewCountService;

    @InjectMocks
    private PostQueryService postQueryService;

    @Test
    @DisplayName("익명 사용자가 게시글을 읽을 때 조회수 증가는 건너뛴다")
    void readPost_whenAnonymous_skipsViewIncrement() {
        Long postId = 1L;
        Post post = Post.builder()
            .id(postId)
            .title("title")
            .content("content")
            .tags(List.of("tag"))
            .accountId(1L)
            .nickname("writer")
            .build();

        given(postReader.getById(postId)).willReturn(post);
        given(viewCountService.getViewCount(postId)).willReturn(3L);
        given(postLikeService.getLikeInfo(postId, null)).willReturn(new LikeResult(false, 1L));
        given(commentQueryService.getCommentCount(postId)).willReturn(2);

        PostDetail response = postQueryService.readPost(postId, null);

        assertThat(response.views()).isEqualTo(3L);
        assertThat(response.isLiked()).isFalse();
        verify(viewCountService, never()).incrementIfNew(postId, null);
    }

    @Test
    @DisplayName("게시글 목록 조회는 외부 호출 없이 좋아요·조회수·댓글 수를 한 번에 묶어 채운다")
    void getPosts_batchesCountsWithoutExternalCalls() {
        Post post = Post.builder()
            .id(3L)
            .title("title")
            .content("content")
            .accountId(1L)
            .nickname("writer")
            .build();
        given(postStore.findByKeyword(null, Pageable.unpaged())).willReturn(new PageImpl<>(List.of(post)));
        given(postLikeService.getLikedPostIds(List.of(3L), null)).willReturn(Set.of());
        given(viewCountService.getViewCounts(List.of(3L))).willReturn(Map.of(3L, 5L));
        given(postLikeService.getLikeCounts(List.of(3L))).willReturn(Map.of(3L, 0L));
        given(commentQueryService.getCommentCounts(List.of(3L))).willReturn(Map.of(3L, 0));

        PostDetail response = postQueryService.getPosts(null, Pageable.unpaged(), null).getContent().getFirst();

        assertThat(response.views()).isEqualTo(5L);
        assertThat(response.commentCount()).isZero();
    }

    @Test
    @DisplayName("게시글 목록 조회는 앞뒤 공백을 지운 검색어를 store에 넘긴다")
    void getPosts_trimsKeywordBeforeDelegating() {
        Pageable pageable = Pageable.unpaged();
        given(postStore.findByKeyword("격리 수준", pageable)).willReturn(new PageImpl<>(List.of()));

        postQueryService.getPosts("  격리 수준  ", pageable, null);

        verify(postStore).findByKeyword("격리 수준", pageable);
    }
}
