package dev.iamrat.board.post;

import java.util.List;

public record PostWriteCommand(
    String title,
    String content,
    String summary,
    List<String> tags,
    String userId,
    String nickname,
    PostCategory category
) {
}
