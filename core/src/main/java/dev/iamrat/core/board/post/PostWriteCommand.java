package dev.iamrat.core.board.post;

import java.util.List;

public record PostWriteCommand(
    String title,
    String content,
    String summary,
    List<String> tags,
    Long accountId,
    String nickname,
    PostCategory category,
    PostPublishOrigin publishOrigin
) {
    public PostWriteCommand(
        String title,
        String content,
        String summary,
        List<String> tags,
        Long accountId,
        String nickname,
        PostCategory category
    ) {
        this(title, content, summary, tags, accountId, nickname, category, PostPublishOrigin.USER);
    }

    public PostWriteCommand {
        if (publishOrigin == null) {
            publishOrigin = PostPublishOrigin.USER;
        }
    }
}
