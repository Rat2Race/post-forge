package dev.iamrat.ai.chat.application;

import dev.iamrat.ai.search.domain.SearchResult;
import java.util.List;
import java.util.stream.Collectors;

final class TestChatPromptTemplate implements ChatPromptTemplate {

    @Override
    public String chatSystemPrompt(List<SearchResult> context) {
        String contextSuffix = context.isEmpty()
            ? ""
            : "\n\n참고할 컨텍스트:\n" + context.stream()
                .map(SearchResult::text)
                .collect(Collectors.joining("\n\n"));
        return "chat-system" + contextSuffix;
    }
}
