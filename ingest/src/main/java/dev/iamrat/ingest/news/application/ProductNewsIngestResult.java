package dev.iamrat.ingest.news.application;

import dev.iamrat.ingest.document.application.DocumentIngestResult;
import java.util.List;

public record ProductNewsIngestResult(
    String keyword,
    Long productId,
    List<String> queries,
    DocumentIngestResult ingestResult
) {

    public ProductNewsIngestResult {
        queries = queries == null ? List.of() : List.copyOf(queries);
    }

    public int newsCount() {
        return ingestResult.documentCount();
    }
}
