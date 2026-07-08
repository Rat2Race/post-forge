package dev.iamrat.ingest.pipeline.application;

public record DocumentStoreResult(
    int chunkCount,
    boolean embeddingsStored,
    String degradationReason
) {
    public static DocumentStoreResult stored(int chunkCount) {
        return new DocumentStoreResult(chunkCount, true, null);
    }

    public static DocumentStoreResult acceptedWithoutEmbeddings(int chunkCount, String reason) {
        return new DocumentStoreResult(chunkCount, false, reason);
    }
}
