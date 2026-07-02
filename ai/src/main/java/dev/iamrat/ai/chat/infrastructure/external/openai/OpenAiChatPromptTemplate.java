package dev.iamrat.ai.chat.infrastructure.external.openai;

import dev.iamrat.ai.chat.application.ChatPromptTemplate;
import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.ai.support.application.PromptTemplateLoader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiChatPromptTemplate implements ChatPromptTemplate {

    private static final String SAFETY_POLICY_PROMPT_PATH = "prompts/safety-policy.md";
    private static final String CHAT_SYSTEM_PROMPT_PATH = "prompts/chat-system.md";
    private static final String NO_RELATED_CONTEXT_SUFFIX = """

        관련 참고 문서 없음:
        검색은 정상 처리되었지만 질문과 직접 관련된 저장 문서를 찾지 못했습니다. 저장 문서에 근거가 없는 구체적인 사실은 단정하지 말고, 일반 정보로 답변할 때는 근거 문서가 없음을 밝혀주세요.""";

    private final PromptTemplateLoader promptTemplateLoader;

    @Override
    public String chatSystemPrompt(List<SearchResult> context) {
        String contextSuffix = context.isEmpty()
            ? NO_RELATED_CONTEXT_SUFFIX
            : "\n\n참고할 컨텍스트:\n" + context.stream()
                .map(SearchResult::text)
                .collect(Collectors.joining("\n\n"));

        String chatPrompt = promptTemplateLoader.render(
            CHAT_SYSTEM_PROMPT_PATH,
            Map.of("contextSuffix", contextSuffix)
        );
        return String.join(
            "\n\n",
            promptTemplateLoader.load(SAFETY_POLICY_PROMPT_PATH),
            chatPrompt
        ).trim();
    }
}
