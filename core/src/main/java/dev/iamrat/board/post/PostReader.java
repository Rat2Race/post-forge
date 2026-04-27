package dev.iamrat.board.post;

public interface PostReader {
    PostContext read(Long postId);
}
