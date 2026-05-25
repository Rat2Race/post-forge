package dev.iamrat.collector.source.presentation.dto;

public record SourceCollectionTriggerResponse(String message) implements SourceCollectionResponse {

    public static SourceCollectionTriggerResponse of(String message) {
        return new SourceCollectionTriggerResponse(message);
    }
}
