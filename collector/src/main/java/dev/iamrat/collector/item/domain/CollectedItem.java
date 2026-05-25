package dev.iamrat.collector.item.domain;

public record CollectedItem(
    String title,
    String originalLink,
    String link,
    String description,
    String publishedAt
) {
}
