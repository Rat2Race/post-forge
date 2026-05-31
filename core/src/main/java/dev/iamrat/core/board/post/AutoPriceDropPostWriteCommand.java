package dev.iamrat.core.board.post;

import java.time.LocalDate;
import java.util.List;

public record AutoPriceDropPostWriteCommand(
    Long productId,
    String eventId,
    LocalDate postDate,
    String title,
    String content,
    String summary,
    List<String> tags,
    boolean publishNow
) {
}
