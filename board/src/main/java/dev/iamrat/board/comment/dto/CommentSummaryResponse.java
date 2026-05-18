package dev.iamrat.board.comment.dto;

import dev.iamrat.board.comment.entity.Comment;
import java.time.LocalDateTime;

public record CommentSummaryResponse(
    Long id,
    String content,
    String userId,
    String nickname,
    Long parentId,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static CommentSummaryResponse from(Comment comment) {
        return new CommentSummaryResponse(
            comment.getId(),
            comment.getContent(),
            comment.getUserId(),
            comment.getNickname(),
            comment.getParent() != null ? comment.getParent().getId() : null,
            comment.getCreatedAt(),
            comment.getModifiedAt()
        );
    }
}
