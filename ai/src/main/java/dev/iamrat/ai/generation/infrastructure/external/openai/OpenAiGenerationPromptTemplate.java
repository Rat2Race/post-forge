package dev.iamrat.ai.generation.infrastructure.external.openai;

import dev.iamrat.ai.generation.application.AiPromptTemplate;
import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.ai.support.infrastructure.openai.PromptTemplateLoader;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiGenerationPromptTemplate implements AiPromptTemplate {

    private static final String NEWS_ANALYSIS_SYSTEM_PROMPT_PATH = "prompts/news-analysis-system.md";
    private static final String NEWS_ANALYSIS_USER_PROMPT_PATH = "prompts/news-analysis-user.md";

    private final PromptTemplateLoader promptTemplateLoader;

    @Override
    public String newsAnalysisSystemPrompt() {
        return promptTemplateLoader.load(NEWS_ANALYSIS_SYSTEM_PROMPT_PATH);
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
        return promptTemplateLoader.render(
            NEWS_ANALYSIS_USER_PROMPT_PATH,
            Map.of(
                "keyword", safe(keyword),
                "articleTitle", safe(articleTitle),
                "originalLink", safe(originalLink),
                "articleContent", safe(articleContent),
                "relatedNewsSection", renderSection("## 관련 뉴스 묶음", relatedNews),
                "historySection", renderSection("## 과거 유사 뉴스/분석", history)
            )
        );
    }

    private String renderSection(String header, List<SearchResult> results) {
        if (results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        for (SearchResult result : results) {
            sb.append(result.text()).append("\n\n");
        }
        return sb.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
