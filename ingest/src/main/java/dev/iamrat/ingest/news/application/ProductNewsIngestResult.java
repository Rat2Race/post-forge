package dev.iamrat.ingest.news.application;

import java.util.List;

public record ProductNewsIngestResult(
    String keyword,
    List<String> queries,
    int newsCount,
    int chunkCount
) {

    public ProductNewsIngestResult {
        queries = queries == null ? List.of() : List.copyOf(queries);
    }
}
