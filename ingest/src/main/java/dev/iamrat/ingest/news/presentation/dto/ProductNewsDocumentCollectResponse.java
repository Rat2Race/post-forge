package dev.iamrat.ingest.news.presentation.dto;

import dev.iamrat.ingest.news.application.ProductNewsDocumentCollectResult;
import java.util.List;

public record ProductNewsDocumentCollectResponse(
    String keyword,
    Long productId,
    List<String> queries,
    int newsCount,
    int chunkCount,
    boolean embeddingsStored,
    String degradationReason,
    String message
) {

    public static ProductNewsDocumentCollectResponse from(ProductNewsDocumentCollectResult result) {
        boolean embeddingsStored = result.ingestResult().embeddingsStored();
        return new ProductNewsDocumentCollectResponse(
            result.keyword(),
            result.productId(),
            result.queries(),
            result.newsCount(),
            result.ingestResult().chunkCount(),
            embeddingsStored,
            result.ingestResult().degradationReason(),
            embeddingsStored ? "상품 관련 뉴스 문서를 저장했습니다." : "상품 관련 뉴스 문서를 임베딩 없이 접수했습니다."
        );
    }
}
