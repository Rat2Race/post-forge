package dev.iamrat.ai.generation.application;

import dev.iamrat.ai.search.domain.SearchResult;
import java.util.List;
import java.util.stream.Collectors;

public final class TestAiPromptTemplate implements AiPromptTemplate {

    @Override
    public String newsAnalysisSystemPrompt() {
        return "news-analysis-system";
    }

    @Override
    public String newsAnalysisUserPrompt(
        String keyword,
        String articleTitle,
        String articleContent,
        String originalLink,
        List<SearchResult> relatedNews,
        List<SearchResult> history
    ) {
        return String.join("\n",
            "keyword=" + keyword,
            "articleTitle=" + articleTitle,
            "articleContent=" + articleContent,
            "originalLink=" + originalLink,
            renderSection("relatedNews", relatedNews),
            renderSection("history", history)
        );
    }

    private String renderSection(String label, List<SearchResult> results) {
        return label + "=" + results.stream()
            .map(SearchResult::text)
            .collect(Collectors.joining("\n\n"));
    }
}
