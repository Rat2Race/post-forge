package dev.iamrat.ai.draft.infrastructure.prompt;

import dev.iamrat.ai.draft.application.PostDraftPromptTemplate;
import dev.iamrat.ai.support.application.PromptTemplateLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkdownPostDraftPromptTemplate implements PostDraftPromptTemplate {

    private static final String SAFETY_POLICY_PROMPT_PATH = "prompts/safety-policy.md";
    private static final String DRAFT_SYSTEM_PROMPT_PATH = "prompts/draft-system.md";

    private final PromptTemplateLoader promptTemplateLoader;

    @Override
    public String draftSystemPrompt() {
        return String.join(
            "\n\n",
            promptTemplateLoader.load(SAFETY_POLICY_PROMPT_PATH),
            promptTemplateLoader.load(DRAFT_SYSTEM_PROMPT_PATH)
        ).trim();
    }
}
