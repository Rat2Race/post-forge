package dev.iamrat.board.post.domain.event;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.event.EventType;

public record PostDeletedEvent(
    Long postId,
    Long accountId,
    String title
) implements PostDomainEvent {
    public static final String EVENT_TYPE = "PostDeleted";

    public static PostDeletedEvent from(Post post) {
        return new PostDeletedEvent(
            post.getId(),
            post.getAccountId(),
            post.getTitle()
        );
    }

    @Override
    public EventType eventType() {
        return EventType.from(EVENT_TYPE);
    }
}
