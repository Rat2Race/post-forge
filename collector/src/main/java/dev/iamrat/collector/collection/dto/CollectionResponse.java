package dev.iamrat.collector.collection.dto;

public sealed interface CollectionResponse permits CollectionErrorResponse, CollectionTriggerResponse {
}
