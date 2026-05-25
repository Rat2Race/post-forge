package dev.iamrat.ai.generation.application;

import dev.iamrat.ai.search.domain.SearchResult;
import java.util.List;

public interface AiPromptTemplate {
    String newsAnalysisSystemPrompt();

    String newsAnalysisUserPrompt(
        String keyword,
        String articleTitle,
        String articleContent,
        String originalLink,
        List<SearchResult> relatedNews,
        List<SearchResult> history
    );
}
