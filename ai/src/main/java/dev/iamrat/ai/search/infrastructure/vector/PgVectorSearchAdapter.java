package dev.iamrat.ai.search.infrastructure.vector;

import dev.iamrat.ai.search.application.SearchPort;
import dev.iamrat.ai.search.application.SearchOutcome;
import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgVectorSearchAdapter implements SearchPort {

    private final VectorStore vectorStore;
    private final MeterRegistry meterRegistry;

    @Override
    public SearchOutcome searchSimilar(String query, int topK) {
        try {
            List<SearchResult> results = toResults(vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build()
            ));
            counter("ai_vector_search_success_total").increment();
            return SearchOutcome.success(results);
        } catch (RuntimeException exception) {
            counter("ai_vector_search_degraded_total").increment();
            log.warn("Vector similarity search unavailable. reason={}", exception.getMessage());
            return SearchOutcome.unavailable(exception.getMessage());
        }
    }

    @Override
    public SearchOutcome searchBySource(String source, String query, int topK) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var filter = builder.eq(SourceDocumentCommand.SOURCE_METADATA_KEY, source).build();

        try {
            List<SearchResult> results = toResults(vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression(filter)
                    .build()
            ));
            counter("ai_vector_search_success_total").increment();
            return SearchOutcome.success(results);
        } catch (RuntimeException exception) {
            counter("ai_vector_search_degraded_total").increment();
            log.warn("Vector source search unavailable. source={} reason={}", source, exception.getMessage());
            return SearchOutcome.unavailable(exception.getMessage());
        }
    }

    private List<SearchResult> toResults(List<Document> documents) {
        return documents.stream()
            .map(document -> new SearchResult(document.getText(), document.getMetadata()))
            .toList();
    }

    private Counter counter(String name) {
        return Counter.builder(name).register(meterRegistry);
    }
}
