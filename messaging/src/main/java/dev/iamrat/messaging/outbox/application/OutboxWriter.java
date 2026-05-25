package dev.iamrat.messaging.outbox.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.core.event.DomainEventRecorder;
import dev.iamrat.core.event.EventType;
import dev.iamrat.messaging.outbox.domain.EventEnvelope;
import dev.iamrat.messaging.outbox.domain.OutboxMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class OutboxWriter implements DomainEventRecorder {

    private final OutboxMessageStore outboxMessageStore;
    private final ObjectMapper objectMapper;

    public OutboxMessage write(String eventType, String aggregateType, String aggregateId, Object payload) {
        return write(EventEnvelope.from(eventType, aggregateType, aggregateId, payload));
    }

    @Override
    public void record(EventType eventType, String aggregateType, String aggregateId, Object payload) {
        write(EventEnvelope.from(eventType, aggregateType, aggregateId, payload));
    }

    public OutboxMessage write(EventEnvelope envelope) {
        String payload = serialize(envelope.payload());
        OutboxMessage message = OutboxMessage.pending(
            envelope.eventType().value(),
            envelope.aggregateType(),
            envelope.aggregateId(),
            payload
        );
        return outboxMessageStore.save(message);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Outbox payload must be JSON serializable", e);
        }
    }
}
