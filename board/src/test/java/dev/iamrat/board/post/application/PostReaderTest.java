package dev.iamrat.board.post.application;

import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PostReaderTest {

    @Mock
    private PostStore postStore;

    @InjectMocks
    private PostReader postReader;

    @Test
    @DisplayName("게시글이 없으면 POST_NOT_FOUND 예외를 던진다")
    void getById_whenPostMissing_throwsNotFound() {
        given(postStore.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postReader.getById(1L))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(BoardErrorCode.POST_NOT_FOUND);
    }

}
