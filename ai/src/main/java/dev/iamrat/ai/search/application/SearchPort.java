package dev.iamrat.ai.search.application;

import java.util.List;

public interface SearchPort {
    List<String> searchSimilar(String query, int topK);
}
