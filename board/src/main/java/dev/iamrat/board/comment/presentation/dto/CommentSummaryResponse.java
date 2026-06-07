package dev.iamrat.board.comment.presentation.dto;

import dev.iamrat.board.comment.application.CommentSummaryResult;
import dev.iamrat.board.comment.domain.Comment;
import java.time.LocalDateTime;

public record CommentSummaryResponse(
    Long id,
    String content,
    Long accountId,
    String nickname,
    Long parentId,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static CommentSummaryResponse from(Comment comment) {
        return new CommentSummaryResponse(
            comment.getId(),
            comment.getContent(),
            comment.getAccountId(),
            comment.getNickname(),
            comment.getParent() != null ? comment.getParent().getId() : null,
            comment.getCreatedAt(),
            comment.getModifiedAt()
        );
    }

    public static CommentSummaryResponse from(CommentSummaryResult result) {
        return new CommentSummaryResponse(
            result.id(),
            result.content(),
            result.accountId(),
            result.nickname(),
            result.parentId(),
            result.createdAt(),
            result.modifiedAt()
        );
    }
}
