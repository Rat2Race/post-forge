package dev.iamrat.core.event;

import java.time.Instant;

public interface DomainEvent {

    String getEventId();

    String getEventType();

    String getAggregateType();

    String getAggregateId();

    String getPayload();

    Instant getOccurredAt();

    default EventType type() {
        return EventType.from(getEventType());
    }
}
