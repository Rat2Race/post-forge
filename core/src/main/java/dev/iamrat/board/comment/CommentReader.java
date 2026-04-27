package dev.iamrat.board.comment;

public interface CommentReader {
    CommentContext read(Long commentId);
}
