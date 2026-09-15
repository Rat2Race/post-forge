package dev.iamrat.board.post.domain;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostPolicyTest {

    private final PostPolicy postPolicy = new PostPolicy();

    @Test
    @DisplayName("작성자가 null이면 잘못된 입력 예외를 던진다")
    void validateAuthor_nullAccount_throwsInvalidInput() {
        assertThatThrownBy(() -> postPolicy.validateAuthor(null))
            .isInstanceOf(CustomException.class)
            .extracting(error -> ((CustomException) error).getErrorCode())
            .isEqualTo(CommonErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("게시글 작성자 ID와 계정 ID가 같으면 소유자로 판단한다")
    void isOwner_matchesPostAccountId() {
        Post post = Post.general("title", "content", 1L, "writer");

        assertThat(postPolicy.isOwner(post, 1L)).isTrue();
        assertThat(postPolicy.isOwner(post, 2L)).isFalse();
        assertThat(postPolicy.isOwner(post, null)).isFalse();
    }
}
