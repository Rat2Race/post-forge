package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeTargetService {

    private final CommentStore commentStore;

    public Comment getReference(Long commentId) {
        if (commentId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
        return commentStore.getReferenceById(commentId);
    }

    @Transactional
    public void updateLikeCount(Long commentId, long likeCount) {
        if (commentId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
        commentStore.updateLikeCount(commentId, likeCount);
    }
}
