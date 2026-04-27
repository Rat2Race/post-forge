package dev.iamrat.board.post;

import java.util.List;

public interface PostWriter {
    Long write(String title, String content, String summary, List<String> tags, String userId, String nickname);
}
