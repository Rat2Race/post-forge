package dev.iamrat.board.post.domain.event;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.event.EventType;

public record PostCreatedEvent(
    Long postId,
    String title,
    Long accountId,
    String nickname,
    PostCategory category
) implements PostDomainEvent {
    public static final String EVENT_TYPE = "PostCreated";

    public static PostCreatedEvent from(Post post) {
        return new PostCreatedEvent(
            post.getId(),
            post.getTitle(),
            post.getAccountId(),
            post.getNickname(),
            post.getCategory()
        );
    }

    @Override
    public EventType eventType() {
        return EventType.from(EVENT_TYPE);
    }
}
