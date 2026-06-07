package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
import java.time.LocalDateTime;

public record CommentDetailResult(
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
    public static CommentDetailResult from(Comment comment, Long likeCount, boolean isLiked) {
        return new CommentDetailResult(
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
