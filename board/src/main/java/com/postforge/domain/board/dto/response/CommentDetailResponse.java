package com.postforge.domain.board.dto.response;

import com.postforge.domain.board.entity.Comment;
import java.time.LocalDateTime;

public record CommentDetailResponse(
    Long id,
    String content,
    String userId,
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
            comment.getUserId(),
            comment.getParent() != null ? comment.getParent().getId() : null,
            comment.getReplies() != null ? comment.getReplies().size() : 0,
            likeCount,
            isLiked,
            comment.getCreatedAt(),
            comment.getModifiedAt()
        );
    }
}
