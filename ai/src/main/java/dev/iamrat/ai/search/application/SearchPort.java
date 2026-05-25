package dev.iamrat.ai.search.application;

import dev.iamrat.ai.search.domain.SearchResult;
import java.util.List;

public interface SearchPort {
    List<SearchResult> searchSimilar(String query, int topK);

    List<SearchResult> searchBySource(String source, String query, int topK);
}
