package dev.iamrat.ai.search.application;

import dev.iamrat.ai.search.domain.SearchResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final SearchPort searchPort;

    public List<SearchResult> searchSimilar(String query, int topK) {
        return searchPort.searchSimilar(query, topK);
    }

    public List<SearchResult> searchBySource(String source, String query, int topK) {
        return searchPort.searchBySource(source, query, topK);
    }
}
