package dev.iamrat.post.service;

import dev.iamrat.comment.service.CommentService;
import dev.iamrat.file.repository.FileRepository;
import dev.iamrat.like.dto.LikeResponse;
import dev.iamrat.like.post.service.PostLikeService;
import dev.iamrat.like.support.LikeRequestGuard;
import dev.iamrat.post.dto.PostDetailResponse;
import dev.iamrat.post.entity.Post;
import dev.iamrat.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeService postLikeService;

    @Mock
    private LikeRequestGuard likeRequestGuard;

    @Mock
    private CommentService commentService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ViewCountService viewCountService;

    @InjectMocks
    private PostService postService;

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
            .userId("author")
            .nickname("writer")
            .build();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(viewCountService.getViewCount(postId)).willReturn(3L);
        given(postLikeService.getLikeInfo(postId, null)).willReturn(new LikeResponse(false, 1L));
        given(commentService.getCommentCount(postId)).willReturn(2);

        PostDetailResponse response = postService.readPost(postId, null);

        assertThat(response.views()).isEqualTo(3L);
        assertThat(response.isLiked()).isFalse();
        verify(viewCountService, never()).incrementIfNew(postId, null);
    }
}
