package dev.iamrat.comment;

public interface CommentWriter {
    Long write(Long postId, Long parentId, String content, String userId, String nickname);
}
