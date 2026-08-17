package dev.iamrat.board.comment.application;

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
class CommentReaderTest {

    @Mock
    private CommentStore commentStore;

    @InjectMocks
    private CommentReader commentReader;

    @Test
    @DisplayName("댓글이 없으면 COMMENT_NOT_FOUND 예외를 던진다")
    void getById_whenCommentMissing_throwsNotFound() {
        given(commentStore.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentReader.getById(1L))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(BoardErrorCode.COMMENT_NOT_FOUND);
    }
}
