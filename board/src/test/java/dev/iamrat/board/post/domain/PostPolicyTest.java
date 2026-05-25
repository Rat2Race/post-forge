package dev.iamrat.board.post.domain;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostPolicyTest {

    private final PostPolicy postPolicy = new PostPolicy();

    @Test
    void validateAuthor_nullAccount_throwsInvalidInput() {
        assertThatThrownBy(() -> postPolicy.validateAuthor(null))
            .isInstanceOf(CustomException.class)
            .extracting(error -> ((CustomException) error).getErrorCode())
            .isEqualTo(CommonErrorCode.INVALID_INPUT);
    }

    @Test
    void isOwner_matchesPostAccountId() {
        Post post = Post.general("title", "content", 1L, "writer");

        assertThat(postPolicy.isOwner(post, 1L)).isTrue();
        assertThat(postPolicy.isOwner(post, 2L)).isFalse();
        assertThat(postPolicy.isOwner(post, null)).isFalse();
    }
}
