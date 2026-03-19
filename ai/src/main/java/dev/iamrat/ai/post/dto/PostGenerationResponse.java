package dev.iamrat.ai.post.dto;

public record PostGenerationResponse(
        GeneratedPost post,
        Long postId
) {
}
