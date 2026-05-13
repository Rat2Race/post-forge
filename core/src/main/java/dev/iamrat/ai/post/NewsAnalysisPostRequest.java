package dev.iamrat.ai.post;

public record NewsAnalysisPostRequest(
        String keyword,
        String articleTitle,
        String articleContent,
        String originalLink
) {
}
