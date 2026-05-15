package dev.iamrat.common.dto;

public record CrawlErrorResponse(
    String error,
    String available
) implements CrawlResponse {
}
