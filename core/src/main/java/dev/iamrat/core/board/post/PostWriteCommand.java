package dev.iamrat.core.board.post;

import java.util.List;

public record PostWriteCommand(
    String title,
    String content,
    String summary,
    List<String> tags,
    Long accountId,
    String nickname,
    PostCategory category
) {
}
