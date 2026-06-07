package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
import java.time.LocalDateTime;

public record CommentSummaryResult(
    Long id,
    String content,
    Long accountId,
    String nickname,
    Long parentId,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static CommentSummaryResult from(Comment comment) {
        return new CommentSummaryResult(
            comment.getId(),
            comment.getContent(),
            comment.getAccountId(),
            comment.getNickname(),
            comment.getParent() != null ? comment.getParent().getId() : null,
            comment.getCreatedAt(),
            comment.getModifiedAt()
        );
    }
}
