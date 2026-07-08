package dev.iamrat.ai.draft.presentation.dto;

import dev.iamrat.core.board.post.PostCategory;
import java.util.List;

public record PostDraftResponse(
    String title,
    String content,
    String summary,
    List<String> tags,
    PostCategory category
) {
}
