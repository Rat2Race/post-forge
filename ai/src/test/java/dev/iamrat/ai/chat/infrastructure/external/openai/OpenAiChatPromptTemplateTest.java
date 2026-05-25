package dev.iamrat.ai.chat.infrastructure.external.openai;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.ai.support.infrastructure.openai.PromptTemplateLoader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenAiChatPromptTemplateTest {

    private final OpenAiChatPromptTemplate promptTemplate = new OpenAiChatPromptTemplate(new PromptTemplateLoader());

    @Test
    @DisplayName("채팅 시스템 프롬프트에 검색 컨텍스트를 덧붙인다")
    void chatSystemPrompt_appendsSearchContext() {
        String prompt = promptTemplate.chatSystemPrompt(List.of(
            new SearchResult("첫 번째 문맥", Map.of()),
            new SearchResult("두 번째 문맥", Map.of())
        ));

        assertThat(prompt)
            .contains("당신은 PostForge 커뮤니티의 AI 어시스턴트입니다.")
            .contains("참고할 컨텍스트:")
            .contains("첫 번째 문맥")
            .contains("두 번째 문맥")
            .doesNotContain("{{contextSuffix}}");
    }
}
