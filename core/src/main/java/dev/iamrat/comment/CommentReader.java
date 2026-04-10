package dev.iamrat.comment;

public interface CommentReader {
    CommentContext read(Long commentId);
}
