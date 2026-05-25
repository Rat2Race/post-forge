package dev.iamrat.board.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PostViewCountServiceTest {

    @Mock
    private PostStore postStore;

    @InjectMocks
    private PostViewCountService postViewCountService;

    @Test
    @DisplayName("postId로 현재 DB 조회수를 조회한다")
    void getViewCount_existingPost_returnsViews() {
        given(postStore.findById(1L)).willReturn(Optional.of(post(1L, 12L)));

        assertThat(postViewCountService.getViewCount(1L)).isEqualTo(12L);
    }

    @Test
    @DisplayName("없는 게시글 조회수 조회는 POST_NOT_FOUND 예외를 던진다")
    void getViewCount_missingPost_throwsPostNotFound() {
        given(postStore.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postViewCountService.getViewCount(1L))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(BoardErrorCode.POST_NOT_FOUND));
    }

    @Test
    @DisplayName("postIds로 현재 DB 조회수를 일괄 조회한다")
    void findViewCounts_existingPosts_returnsIdToViews() {
        given(postStore.findAllById(List.of(1L, 2L)))
            .willReturn(List.of(post(1L, 12L), post(2L, 7L)));

        assertThat(postViewCountService.findViewCounts(List.of(1L, 2L)))
            .isEqualTo(Map.of(1L, 12L, 2L, 7L));
    }

    @Test
    @DisplayName("postId와 조회수로 DB 조회수 값을 갱신한다")
    void updateViewCount_delegatesRepository() {
        postViewCountService.updateViewCount(1L, 12L);

        verify(postStore).updateViews(1L, 12L);
    }

    private static Post post(Long id, Long views) {
        return Post.builder()
            .id(id)
            .title("title")
            .content("content")
            .accountId(1L)
            .nickname("rat")
            .views(views)
            .build();
    }
}
