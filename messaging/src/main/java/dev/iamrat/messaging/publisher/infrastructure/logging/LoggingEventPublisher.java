package dev.iamrat.messaging.publisher.infrastructure.logging;

import dev.iamrat.core.event.DomainEvent;
import dev.iamrat.core.event.EventType;
import dev.iamrat.messaging.publisher.application.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "postforge.messaging.publisher.logging",
    name = "enabled",
    havingValue = "true"
)
public class LoggingEventPublisher implements EventPublisher {

    @Override
    public boolean supports(EventType eventType) {
        return true;
    }

    @Override
    public void publish(DomainEvent message) {
        log.info(
            "Outbox message published through logging adapter. eventId={}, eventType={}, aggregateType={}, aggregateId={}",
            message.getEventId(),
            message.getEventType(),
            message.getAggregateType(),
            message.getAggregateId()
        );
    }
}
