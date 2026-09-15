package dev.iamrat.board.comment.domain;

import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;

public class CommentPolicy {

    public void validateAuthor(Long accountId) {
        if (accountId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
    }

    public void validateParent(Long postId, Comment parent) {
        if (!parent.getPost().getId().equals(postId)) {
            throw new CustomException(BoardErrorCode.INVALID_COMMENT_PARENT);
        }

        if (parent.getParent() != null) {
            throw new CustomException(BoardErrorCode.MAX_COMMENT_DEPTH_EXCEEDED);
        }
    }

    public boolean isOwner(Comment comment, Long accountId) {
        return accountId != null && accountId.equals(comment.getAccountId());
    }
}
