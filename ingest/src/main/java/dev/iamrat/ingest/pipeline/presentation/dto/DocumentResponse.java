package dev.iamrat.ingest.pipeline.presentation.dto;

public record DocumentResponse(
    int count,
    int chunkCount,
    boolean embeddingsStored,
    String degradationReason,
    String message
) {
}
