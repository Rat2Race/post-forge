package dev.iamrat.ai.search.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final SearchPort searchPort;

    public SearchOutcome searchSimilar(String query, int topK) {
        return searchPort.searchSimilar(query, topK);
    }

    public SearchOutcome searchBySource(String source, String query, int topK) {
        return searchPort.searchBySource(source, query, topK);
    }
}
