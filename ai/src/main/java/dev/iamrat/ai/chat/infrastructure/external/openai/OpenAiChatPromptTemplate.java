package dev.iamrat.ai.chat.infrastructure.external.openai;

import dev.iamrat.ai.chat.application.ChatPromptTemplate;
import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.ai.support.infrastructure.openai.PromptTemplateLoader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiChatPromptTemplate implements ChatPromptTemplate {

    private static final String CHAT_SYSTEM_PROMPT_PATH = "prompts/chat-system.md";

    private final PromptTemplateLoader promptTemplateLoader;

    @Override
    public String chatSystemPrompt(List<SearchResult> context) {
        String contextSuffix = context.isEmpty()
            ? ""
            : "\n\n참고할 컨텍스트:\n" + context.stream()
                .map(SearchResult::text)
                .collect(Collectors.joining("\n\n"));

        return promptTemplateLoader.render(
            CHAT_SYSTEM_PROMPT_PATH,
            Map.of("contextSuffix", contextSuffix)
        );
    }
}
