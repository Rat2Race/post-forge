package dev.iamrat.ai.draft.infrastructure.prompt;

import dev.iamrat.ai.draft.application.LaunchNewsPostDraftPromptTemplate;
import dev.iamrat.ai.support.application.PromptTemplateLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkdownLaunchNewsPostDraftPromptTemplate implements LaunchNewsPostDraftPromptTemplate {

    private static final String LAUNCH_NEWS_DRAFT_SYSTEM_PROMPT_PATH = "prompts/launch-news-draft-system.md";

    private final PromptTemplateLoader promptTemplateLoader;

    @Override
    public String launchNewsSystemPrompt() {
        // Preserve the existing launch-news prompt contract; AiSafetyGuard remains the runtime safety boundary.
        return promptTemplateLoader.load(LAUNCH_NEWS_DRAFT_SYSTEM_PROMPT_PATH);
    }
}
