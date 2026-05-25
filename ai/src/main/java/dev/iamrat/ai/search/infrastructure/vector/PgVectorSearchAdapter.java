package dev.iamrat.ai.search.infrastructure.vector;

import dev.iamrat.ai.search.application.SearchPort;
import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

@Component
public class PgVectorSearchAdapter implements SearchPort {

    private final VectorStore vectorStore;

    public PgVectorSearchAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<SearchResult> searchSimilar(String query, int topK) {
        return toResults(vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build()
        ));
    }

    @Override
    public List<SearchResult> searchBySource(String source, String query, int topK) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var filter = builder.eq(SourceDocumentCommand.SOURCE_METADATA_KEY, source).build();

        return toResults(vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(filter)
                .build()
        ));
    }

    private List<SearchResult> toResults(List<Document> documents) {
        return documents.stream()
            .map(document -> new SearchResult(document.getText(), document.getMetadata()))
            .toList();
    }
}
