package dev.iamrat.ai.search.infrastructure.vector;

import dev.iamrat.ai.search.application.SearchPort;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgVectorSearchAdapter implements SearchPort {

    private final VectorStore vectorStore;
    private final MeterRegistry meterRegistry;

    @Override
    public List<String> searchSimilar(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .build();
        List<Document> documents;
        try {
            documents = vectorStore.similaritySearch(request);
        } catch (RuntimeException exception) {
            meterRegistry.counter("ai_vector_search_degraded_total").increment();
            log.warn("Vector similarity search unavailable", exception);
            throw new CustomException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        List<String> results = toResults(documents);
        meterRegistry.counter("ai_vector_search_success_total").increment();
        return results;
    }

    private List<String> toResults(List<Document> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
            .map(Document::getText)
            .toList();
    }
}
