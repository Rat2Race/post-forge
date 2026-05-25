package dev.iamrat.board.comment.domain;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentPolicyTest {

    private final CommentPolicy commentPolicy = new CommentPolicy();

    @Test
    void validateAuthor_nullAccount_throwsInvalidInput() {
        assertThatThrownBy(() -> commentPolicy.validateAuthor(null))
            .isInstanceOf(CustomException.class)
            .extracting(error -> ((CustomException) error).getErrorCode())
            .isEqualTo(CommonErrorCode.INVALID_INPUT);
    }

    @Test
    void validateParent_rejectsParentFromAnotherPost() {
        Comment parent = Comment.create(post(1L), null, "parent", 10L, "writer");

        assertThatThrownBy(() -> commentPolicy.validateParent(2L, parent))
            .isInstanceOf(CustomException.class)
            .extracting(error -> ((CustomException) error).getErrorCode())
            .isEqualTo(BoardErrorCode.INVALID_COMMENT_PARENT);
    }

    @Test
    void validateParent_rejectsNestedParent() {
        Post post = post(1L);
        Comment root = Comment.create(post, null, "root", 10L, "writer");
        Comment reply = Comment.create(post, root, "reply", 11L, "reply-writer");

        assertThatThrownBy(() -> commentPolicy.validateParent(1L, reply))
            .isInstanceOf(CustomException.class)
            .extracting(error -> ((CustomException) error).getErrorCode())
            .isEqualTo(BoardErrorCode.MAX_COMMENT_DEPTH_EXCEEDED);
    }

    @Test
    void isOwner_matchesCommentAccountId() {
        Comment comment = Comment.create(post(1L), null, "content", 2L, "writer");

        assertThat(commentPolicy.isOwner(comment, 2L)).isTrue();
        assertThat(commentPolicy.isOwner(comment, 3L)).isFalse();
        assertThat(commentPolicy.isOwner(comment, null)).isFalse();
    }

    private Post post(Long id) {
        return Post.builder()
            .id(id)
            .title("title")
            .content("content")
            .accountId(1L)
            .nickname("writer")
            .build();
    }
}
