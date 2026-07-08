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
    PostBoardCategory boardCategory,
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
        this(title, content, summary, tags, accountId, nickname, category, PostBoardCategory.GENERAL);
    }

    public PostWriteCommand(
        String title,
        String content,
        String summary,
        List<String> tags,
        Long accountId,
        String nickname,
        PostCategory category,
        PostPublishOrigin publishOrigin
    ) {
        this(title, content, summary, tags, accountId, nickname, category, PostBoardCategory.GENERAL, publishOrigin);
    }

    public PostWriteCommand(
        String title,
        String content,
        String summary,
        List<String> tags,
        Long accountId,
        String nickname,
        PostCategory category,
        PostBoardCategory boardCategory
    ) {
        this(title, content, summary, tags, accountId, nickname, category, boardCategory, PostPublishOrigin.USER);
    }

    public PostWriteCommand {
        if (boardCategory == null) {
            boardCategory = PostBoardCategory.GENERAL;
        }
        if (publishOrigin == null) {
            publishOrigin = PostPublishOrigin.USER;
        }
    }
}
