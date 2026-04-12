package dev.iamrat.board.comment;

public record CommentContext(
    Long id,
    Long postId,
    Long parentId,
    String content,
    String userId,
    String nickname
) {
}
