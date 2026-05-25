package dev.iamrat.collector.source.presentation.dto;

public record SourceCollectionErrorResponse(
    String error,
    String available
) implements SourceCollectionResponse {
}
