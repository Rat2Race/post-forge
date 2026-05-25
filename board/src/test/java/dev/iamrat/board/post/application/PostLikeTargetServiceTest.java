package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PostLikeTargetServiceTest {

    @Mock
    private PostStore postStore;

    @InjectMocks
    private PostLikeTargetService postLikeTargetService;

    @Test
    @DisplayName("좋아요 생성을 위한 게시글 참조를 repository에서 가져온다")
    void getReference_returnsPostReference() {
        Post post = Post.builder().id(1L).title("title").content("content").accountId(9L).build();
        given(postStore.getReferenceById(1L)).willReturn(post);

        Post result = postLikeTargetService.getReference(1L);

        assertThat(result).isSameAs(post);
    }

    @Test
    @DisplayName("게시글 좋아요 수 갱신을 repository에 위임한다")
    void updateLikeCount_delegatesToRepository() {
        postLikeTargetService.updateLikeCount(1L, 5L);

        verify(postStore).updateLikeCount(1L, 5L);
    }

    @Test
    @DisplayName("게시글 ID가 없으면 입력 오류를 던진다")
    void getReference_whenPostIdIsNull_throwsInvalidInput() {
        assertThatThrownBy(() -> postLikeTargetService.getReference(null))
            .isInstanceOf(CustomException.class);
    }
}
