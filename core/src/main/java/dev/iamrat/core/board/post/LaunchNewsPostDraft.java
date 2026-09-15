package dev.iamrat.core.board.post;

import java.util.List;

public record LaunchNewsPostDraft(
    String title,
    String content,
    String summary,
    List<String> tags
) {
    public LaunchNewsPostDraft {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
