package dev.iamrat.board.post.domain;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;

public class PostPolicy {

    public void validateAuthor(Long accountId) {
        if (accountId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
    }

    public boolean isOwner(Post post, Long accountId) {
        return accountId != null && accountId.equals(post.getAccountId());
    }
}
