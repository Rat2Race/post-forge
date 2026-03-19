package dev.iamrat.ai.post.dto;

import java.util.List;

public record GeneratedPost(
        String title,
        String summary,
        String content,
        List<String> tags
) {
}
