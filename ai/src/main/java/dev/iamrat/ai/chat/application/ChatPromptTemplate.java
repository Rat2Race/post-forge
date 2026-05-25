package dev.iamrat.ai.chat.application;

import dev.iamrat.ai.search.domain.SearchResult;
import java.util.List;

public interface ChatPromptTemplate {
    String chatSystemPrompt(List<SearchResult> context);
}
