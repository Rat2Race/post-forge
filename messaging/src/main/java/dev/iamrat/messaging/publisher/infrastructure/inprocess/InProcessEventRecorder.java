package dev.iamrat.messaging.publisher.infrastructure.inprocess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.core.event.DomainEvent;
import dev.iamrat.core.event.DomainEventRecorder;
import dev.iamrat.core.event.EventPublisher;
import dev.iamrat.core.event.EventType;
import dev.iamrat.messaging.support.error.MessagingExceptionMessages;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class InProcessEventRecorder implements DomainEventRecorder {

    private final List<EventPublisher> eventPublishers;
    private final ObjectMapper objectMapper;

    @Override
    public void record(EventType eventType, String aggregateType, String aggregateId, Object payload) {
        if (eventPublishers.isEmpty()) {
            return;
        }
        DomainEvent event = ImmediateDomainEvent.create(
            eventType.value(),
            aggregateType,
            aggregateId,
            serialize(payload)
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(eventType, event);
                }
            });
            return;
        }
        publish(eventType, event);
    }

    private void publish(EventType eventType, DomainEvent event) {
        eventPublishers.stream()
            .filter(publisher -> publisher.supports(eventType))
            .forEach(publisher -> publisher.publish(event));
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                MessagingExceptionMessages.IN_PROCESS_EVENT_PAYLOAD_MUST_BE_JSON_SERIALIZABLE,
                e
            );
        }
    }

    private record ImmediateDomainEvent(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String payload,
        Instant occurredAt
    ) implements DomainEvent {

        static ImmediateDomainEvent create(
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload
        ) {
            return new ImmediateDomainEvent(
                UUID.randomUUID().toString(),
                eventType,
                aggregateType,
                aggregateId,
                payload,
                Instant.now()
            );
        }

        @Override
        public String getEventId() {
            return eventId;
        }

        @Override
        public String getEventType() {
            return eventType;
        }

        @Override
        public String getAggregateType() {
            return aggregateType;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }

        @Override
        public String getPayload() {
            return payload;
        }

        @Override
        public Instant getOccurredAt() {
            return occurredAt;
        }
    }
}
