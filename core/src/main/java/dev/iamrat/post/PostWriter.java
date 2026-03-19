package dev.iamrat.post;

public interface PostWriter {
    Long write(String title, String content, String userId);
}
