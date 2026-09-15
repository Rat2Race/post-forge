package dev.iamrat.core.board.post;

import java.util.List;

public record DailyDigestDraft(String content, List<String> tags) {
    public DailyDigestDraft {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
