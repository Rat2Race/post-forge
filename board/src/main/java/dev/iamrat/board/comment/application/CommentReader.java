package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentReader {

    private final CommentStore commentStore;

    public Comment getById(Long commentId) {
        if (commentId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
        return commentStore.findById(commentId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.COMMENT_NOT_FOUND));
    }

    public void requireExists(Long commentId) {
        getById(commentId);
    }
}
