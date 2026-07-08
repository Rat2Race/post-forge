package dev.iamrat.ai.draft.infrastructure.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.ai.support.application.PromptTemplateLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MarkdownLaunchNewsPostDraftPromptTemplateTest {

    private static final String EXPECTED_SYSTEM_PROMPT = """
        Write a concise Korean public board post about a verified new-product launch news article.
        Do not invent specs, prices, availability, or purchase recommendations.
        Base the post only on the given title, description, source, and URL.
        """.trim();

    private final MarkdownLaunchNewsPostDraftPromptTemplate promptTemplate =
        new MarkdownLaunchNewsPostDraftPromptTemplate(new PromptTemplateLoader());

    @Test
    @DisplayName("출시 뉴스 초안 시스템 프롬프트를 md 리소스에서 읽는다")
    void launchNewsSystemPrompt_readsMarkdownResource() {
        String prompt = promptTemplate.launchNewsSystemPrompt();

        assertThat(prompt)
            .isEqualTo(EXPECTED_SYSTEM_PROMPT)
            .doesNotContain("{{");
    }
}
