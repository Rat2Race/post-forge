package dev.iamrat.board.comment.presentation.dto;

import dev.iamrat.board.comment.domain.Comment;
import java.time.LocalDateTime;

public record CommentDetailResponse(
    Long id,
    String content,
    Long accountId,
    String nickname,
    Long parentId,
    int replyCount,
    Long likeCount,
    boolean isLiked,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {

    public static CommentDetailResponse from(Comment comment, Long likeCount, boolean isLiked) {
        return new CommentDetailResponse(
            comment.getId(),
            comment.getContent(),
            comment.getAccountId(),
            comment.getNickname(),
            comment.getParent() != null ? comment.getParent().getId() : null,
            comment.getReplies() != null ? comment.getReplies().size() : 0,
            likeCount,
            isLiked,
            comment.getCreatedAt(),
            comment.getModifiedAt()
        );
    }
}
