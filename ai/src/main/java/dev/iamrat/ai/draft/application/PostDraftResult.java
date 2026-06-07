package dev.iamrat.ai.draft.application;

import dev.iamrat.core.board.post.PostCategory;
import java.util.List;

public record PostDraftResult(
    String title,
    String content,
    String summary,
    List<String> tags,
    PostCategory category
) {
}
