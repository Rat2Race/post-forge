package dev.iamrat.ai.comment.dto;

public record AiCommentReplyResponse(
    Long postId,
    Long parentCommentId,
    Long commentId,
    String content
) {
}
