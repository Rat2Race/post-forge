package dev.iamrat.comment.dto;

import dev.iamrat.comment.entity.Comment;
import java.time.LocalDateTime;

public record CommentSummaryResponse(
    Long id,
    String content,
    String userId,
    Long parentId,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static CommentSummaryResponse from(Comment comment) {
        return new CommentSummaryResponse(
            comment.getId(),
            comment.getContent(),
            comment.getUserId(),
            comment.getParent() != null ? comment.getParent().getId() : null,
            comment.getCreatedAt(),
            comment.getModifiedAt()
        );
    }
}
