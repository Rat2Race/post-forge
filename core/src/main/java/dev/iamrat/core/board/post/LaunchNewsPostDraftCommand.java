package dev.iamrat.core.board.post;

public record LaunchNewsPostDraftCommand(
    String keyword,
    String sourceTitle,
    String sourceDescription,
    String canonicalUrl,
    String sourceName,
    String publishedAt
) {
}
