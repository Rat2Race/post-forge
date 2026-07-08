package dev.iamrat.ai.chat.infrastructure.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.ai.support.application.PromptTemplateLoader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MarkdownChatPromptTemplateTest {

    private final MarkdownChatPromptTemplate promptTemplate = new MarkdownChatPromptTemplate(new PromptTemplateLoader());

    @Test
    @DisplayName("채팅 시스템 프롬프트에 검색 컨텍스트를 덧붙인다")
    void chatSystemPrompt_appendsSearchContext() {
        String prompt = promptTemplate.chatSystemPrompt(List.of(
            new SearchResult("첫 번째 문맥", Map.of()),
            new SearchResult("두 번째 문맥", Map.of())
        ));

        assertThat(prompt)
            .contains("공통 보안 정책")
            .contains("당신은 PostForge 커뮤니티의 AI 어시스턴트입니다.")
            .contains("참고할 컨텍스트:")
            .contains("첫 번째 문맥")
            .contains("두 번째 문맥")
            .doesNotContain("{{contextSuffix}}");
    }

    @Test
    @DisplayName("검색은 성공했지만 관련 문서가 없으면 제한 문구를 덧붙인다")
    void chatSystemPrompt_whenNoSearchContext_appendsNoRelatedDocsConstraint() {
        String prompt = promptTemplate.chatSystemPrompt(List.of());

        assertThat(prompt)
            .contains("공통 보안 정책")
            .contains("관련 참고 문서 없음")
            .contains("근거가 없는 구체적인 사실은 단정하지 말고")
            .doesNotContain("참고할 컨텍스트:")
            .doesNotContain("{{contextSuffix}}");
    }
}
