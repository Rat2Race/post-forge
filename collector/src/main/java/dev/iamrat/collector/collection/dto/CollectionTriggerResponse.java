package dev.iamrat.collector.collection.dto;

public record CollectionTriggerResponse(String message) implements CollectionResponse {

    public static CollectionTriggerResponse of(String message) {
        return new CollectionTriggerResponse(message);
    }
}
