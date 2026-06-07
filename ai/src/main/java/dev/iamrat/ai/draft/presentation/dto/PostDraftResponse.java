package dev.iamrat.ai.draft.presentation.dto;

import dev.iamrat.ai.draft.application.PostDraftResult;
import dev.iamrat.core.board.post.PostCategory;
import java.util.List;

public record PostDraftResponse(
    String title,
    String content,
    String summary,
    List<String> tags,
    PostCategory category
) {
    public static PostDraftResponse from(PostDraftResult result) {
        return new PostDraftResponse(
            result.title(),
            result.content(),
            result.summary(),
            result.tags(),
            result.category()
        );
    }
}
