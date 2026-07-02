package dev.iamrat.ingest.news.application;

import dev.iamrat.ingest.pipeline.application.DocumentIngestResult;
import java.util.List;

public record ProductNewsDocumentCollectResult(
    String keyword,
    Long productId,
    List<String> queries,
    int newsCount,
    DocumentIngestResult ingestResult
) {

    public ProductNewsDocumentCollectResult {
        queries = queries == null ? List.of() : List.copyOf(queries);
    }
}
