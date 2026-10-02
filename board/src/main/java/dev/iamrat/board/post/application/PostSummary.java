package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import java.time.LocalDateTime;
import java.util.List;

public record PostSummary(
    Long id,
    String title,
    List<String> tags,
    Long accountId,
    String nickname,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {

    public static PostSummary from(Post post) {
        return new PostSummary(
            post.getId(),
            post.getTitle(),
            List.copyOf(post.getTags()),
            post.getAccountId(),
            post.getNickname(),
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}
