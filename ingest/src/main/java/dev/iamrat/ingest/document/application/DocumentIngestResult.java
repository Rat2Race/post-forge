package dev.iamrat.ingest.document.application;

public record DocumentIngestResult(
    int documentCount,
    int chunkCount
) {
}
