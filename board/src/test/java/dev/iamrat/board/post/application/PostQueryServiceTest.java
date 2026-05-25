package dev.iamrat.board.post.application;

import dev.iamrat.board.comment.application.CommentQueryService;
import dev.iamrat.board.like.application.LikeResponse;
import dev.iamrat.board.like.application.PostLikeService;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.dto.PostDetailResponse;
import dev.iamrat.board.view.application.ViewCountService;
import java.util.List;
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
            .summary("summary")
            .tags(List.of("tag"))
            .accountId(1L)
            .nickname("writer")
            .build();

        given(postReader.getById(postId)).willReturn(post);
        given(viewCountService.getViewCount(postId)).willReturn(3L);
        given(postLikeService.getLikeInfo(postId, null)).willReturn(new LikeResponse(false, 1L));
        given(commentQueryService.getCommentCount(postId)).willReturn(2);

        PostDetailResponse response = postQueryService.readPost(postId, null);

        assertThat(response.views()).isEqualTo(3L);
        assertThat(response.isLiked()).isFalse();
        verify(viewCountService, never()).incrementIfNew(postId, null);
    }
}
