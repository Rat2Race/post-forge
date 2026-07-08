package dev.iamrat.ingest.pipeline.application;

public record DocumentIngestResult(
    int count,
    int chunkCount,
    boolean embeddingsStored,
    String degradationReason
) {
    public static DocumentIngestResult from(int count, DocumentStoreResult storeResult) {
        return new DocumentIngestResult(
            count,
            storeResult.chunkCount(),
            storeResult.embeddingsStored(),
            storeResult.degradationReason()
        );
    }
}
