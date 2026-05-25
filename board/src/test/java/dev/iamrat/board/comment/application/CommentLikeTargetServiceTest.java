package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
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
class CommentLikeTargetServiceTest {

    @Mock
    private CommentStore commentStore;

    @InjectMocks
    private CommentLikeTargetService commentLikeTargetService;

    @Test
    @DisplayName("좋아요 생성을 위한 댓글 참조를 repository에서 가져온다")
    void getReference_returnsCommentReference() {
        Comment comment = Comment.builder()
            .id(1L)
            .post(Post.builder().id(2L).title("title").content("content").accountId(9L).build())
            .content("comment")
            .accountId(3L)
            .build();
        given(commentStore.getReferenceById(1L)).willReturn(comment);

        Comment result = commentLikeTargetService.getReference(1L);

        assertThat(result).isSameAs(comment);
    }

    @Test
    @DisplayName("댓글 좋아요 수 갱신을 repository에 위임한다")
    void updateLikeCount_delegatesToRepository() {
        commentLikeTargetService.updateLikeCount(1L, 5L);

        verify(commentStore).updateLikeCount(1L, 5L);
    }

    @Test
    @DisplayName("댓글 ID가 없으면 입력 오류를 던진다")
    void getReference_whenCommentIdIsNull_throwsInvalidInput() {
        assertThatThrownBy(() -> commentLikeTargetService.getReference(null))
            .isInstanceOf(CustomException.class);
    }
}
