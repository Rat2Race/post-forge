package dev.iamrat.messaging.outbox.domain;

import dev.iamrat.core.event.EventType;
import dev.iamrat.messaging.support.error.MessagingExceptionMessages;

public record EventEnvelope(
    EventType eventType,
    String aggregateType,
    String aggregateId,
    Object payload
) {
    public static EventEnvelope from(
        String eventType,
        String aggregateType,
        String aggregateId,
        Object payload
    ) {
        return EventEnvelope.from(EventType.from(eventType), aggregateType, aggregateId, payload);
    }

    public static EventEnvelope from(
        EventType eventType,
        String aggregateType,
        String aggregateId,
        Object payload
    ) {
        return new EventEnvelope(eventType, aggregateType, aggregateId, payload);
    }

    public EventEnvelope {
        if (eventType == null) {
            throw new IllegalArgumentException(MessagingExceptionMessages.EVENT_TYPE_MUST_NOT_BE_NULL);
        }
        if (payload == null) {
            throw new IllegalArgumentException(MessagingExceptionMessages.PAYLOAD_MUST_NOT_BE_NULL);
        }
    }
}
