package dev.iamrat.ai.draft.infrastructure.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.ai.support.application.PromptTemplateLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MarkdownPostDraftPromptTemplateTest {

    private final MarkdownPostDraftPromptTemplate promptTemplate = new MarkdownPostDraftPromptTemplate(
        new PromptTemplateLoader()
    );

    @Test
    @DisplayName("초안 시스템 프롬프트에 공통 안전 정책과 초안 역할 프롬프트를 함께 포함한다")
    void draftSystemPrompt_combinesSafetyPolicyAndDraftPrompt() {
        String prompt = promptTemplate.draftSystemPrompt();

        assertThat(prompt)
            .contains("공통 보안 정책")
            .contains("PostForge 게시판 초안 작성 도우미")
            .contains("제목, 태그, JSON, 설명 문구를 따로 출력하지 말고 본문만 출력하세요")
            .doesNotContain("{{");
    }
}
