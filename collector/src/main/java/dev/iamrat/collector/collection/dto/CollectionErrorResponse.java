package dev.iamrat.collector.collection.dto;

public record CollectionErrorResponse(
    String error,
    String available
) implements CollectionResponse {
}
