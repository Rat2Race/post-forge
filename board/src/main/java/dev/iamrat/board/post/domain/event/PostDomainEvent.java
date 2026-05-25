package dev.iamrat.board.post.domain.event;

import dev.iamrat.core.event.EventType;

public interface PostDomainEvent {

    String AGGREGATE_TYPE = "post";

    EventType eventType();

    Long postId();

    default String aggregateType() {
        return AGGREGATE_TYPE;
    }

    default String aggregateId() {
        return postId() == null ? null : postId().toString();
    }
}
