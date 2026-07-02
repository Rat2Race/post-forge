package dev.iamrat.ai.search.application;

public interface SearchPort {
    SearchOutcome searchSimilar(String query, int topK);

    SearchOutcome searchBySource(String source, String query, int topK);
}
