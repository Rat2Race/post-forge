package dev.iamrat.core.board.post;

import java.time.LocalDate;
import java.util.List;

public record DailyDigestDraftCommand(
    BoardCategory category,
    LocalDate newsDate,
    List<DailyDigestSourceItem> items
) {
    public DailyDigestDraftCommand {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
