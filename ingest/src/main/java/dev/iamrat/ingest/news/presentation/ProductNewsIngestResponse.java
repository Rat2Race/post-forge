package dev.iamrat.ingest.news.presentation;

import dev.iamrat.ingest.news.application.ProductNewsIngestResult;
import java.util.List;

public record ProductNewsIngestResponse(
    String keyword,
    Long productId,
    List<String> queries,
    int newsCount,
    int chunkCount
) {

    public static ProductNewsIngestResponse from(ProductNewsIngestResult result) {
        return new ProductNewsIngestResponse(
            result.keyword(),
            result.productId(),
            result.queries(),
            result.newsCount(),
            result.ingestResult().chunkCount()
        );
    }
}
