package dev.iamrat.ai.draft.application;

import dev.iamrat.core.board.post.PostCategory;
import java.util.List;

public record PostDraftGenerateCommand(
    String prompt,
    String topic,
    String title,
    String summary,
    List<String> tags,
    PostCategory category
) {
    public String effectivePrompt() {
        if (hasText(prompt)) {
            return prompt.trim();
        }
        return topic.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
