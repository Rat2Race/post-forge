package dev.iamrat.board.post.presentation;

import dev.iamrat.board.post.domain.Post;
import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryResponse(
    Long id,
    String title,
    List<String> tags,
    Long accountId,
    String nickname,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {

    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
            post.getId(),
            post.getTitle(),
            post.getTags(),
            post.getAccountId(),
            post.getNickname(),
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}
