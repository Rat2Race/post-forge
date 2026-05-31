package dev.iamrat.ai.draft.presentation.dto;

import dev.iamrat.core.board.post.PostCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostDraftGenerateRequest(
    @Size(max = 1000, message = "프롬프트는 1000자 이하여야 합니다")
    String prompt,

    @Size(max = 200, message = "주제는 200자 이하여야 합니다")
    String topic,

    @Size(max = 100, message = "제목은 100자 이하여야 합니다")
    String title,

    @Size(max = 500, message = "요약은 500자 이하여야 합니다")
    String summary,

    @Size(max = 20, message = "태그는 최대 20개까지 사용할 수 있습니다")
    List<@Size(max = 50, message = "태그는 50자 이하여야 합니다") String> tags,

    PostCategory category
) {
    @AssertTrue(message = "프롬프트 또는 주제를 입력해주세요.")
    public boolean hasPromptOrTopic() {
        return hasText(prompt) || hasText(topic);
    }

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
