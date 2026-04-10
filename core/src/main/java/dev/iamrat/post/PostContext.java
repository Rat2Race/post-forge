package dev.iamrat.post;

import java.util.List;

public record PostContext(
    Long id,
    String title,
    String content,
    String summary,
    List<String> tags,
    String userId,
    String nickname
) {
}
