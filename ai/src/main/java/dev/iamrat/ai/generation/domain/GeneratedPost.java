package dev.iamrat.ai.generation.domain;

import java.util.List;

public record GeneratedPost(
        String title,
        String summary,
        String content,
        List<String> tags
) {
}
