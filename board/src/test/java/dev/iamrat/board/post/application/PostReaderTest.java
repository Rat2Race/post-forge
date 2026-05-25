package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PostReaderTest {

    @Mock
    private PostStore postStore;

    @InjectMocks
    private PostReader postReader;

    @Test
    @DisplayName("게시글 ID로 게시글을 조회한다")
    void getById_returnsPost() {
        Post post = post(1L);
        given(postStore.findById(1L)).willReturn(Optional.of(post));

        Post result = postReader.getById(1L);

        assertThat(result).isSameAs(post);
    }

    @Test
    @DisplayName("게시글이 없으면 POST_NOT_FOUND 예외를 던진다")
    void getById_whenPostMissing_throwsNotFound() {
        given(postStore.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postReader.getById(1L))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(BoardErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 존재 여부를 확인한다")
    void requireExists_whenPostExists_doesNotThrow() {
        given(postStore.existsById(1L)).willReturn(true);

        postReader.requireExists(1L);
    }

    private Post post(Long postId) {
        return Post.builder()
            .id(postId)
            .title("title")
            .content("content")
            .accountId(1L)
            .nickname("writer")
            .build();
    }
}
