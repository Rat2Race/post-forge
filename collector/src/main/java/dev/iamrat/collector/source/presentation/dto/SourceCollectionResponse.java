package dev.iamrat.collector.source.presentation.dto;

public sealed interface SourceCollectionResponse permits SourceCollectionErrorResponse, SourceCollectionTriggerResponse {
}
