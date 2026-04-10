package dev.iamrat.post;

public interface PostReader {
    PostContext read(Long postId);
}
