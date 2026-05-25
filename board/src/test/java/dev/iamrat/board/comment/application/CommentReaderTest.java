package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
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
class CommentReaderTest {

    @Mock
    private CommentStore commentStore;

    @InjectMocks
    private CommentReader commentReader;

    @Test
    @DisplayName("댓글 ID로 댓글을 조회한다")
    void getById_returnsComment() {
        Comment comment = comment(1L);
        given(commentStore.findById(1L)).willReturn(Optional.of(comment));

        Comment result = commentReader.getById(1L);

        assertThat(result).isSameAs(comment);
    }

    @Test
    @DisplayName("댓글이 없으면 COMMENT_NOT_FOUND 예외를 던진다")
    void getById_whenCommentMissing_throwsNotFound() {
        given(commentStore.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentReader.getById(1L))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(BoardErrorCode.COMMENT_NOT_FOUND);
    }

    private Comment comment(Long commentId) {
        return Comment.builder()
            .id(commentId)
            .post(Post.builder().id(1L).title("title").content("content").accountId(1L).build())
            .content("comment")
            .accountId(2L)
            .nickname("writer")
            .build();
    }
}
