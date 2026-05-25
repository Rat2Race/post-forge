package dev.iamrat.messaging.outbox.application;

import dev.iamrat.core.event.EventType;
import dev.iamrat.messaging.outbox.domain.OutboxMessage;
import dev.iamrat.messaging.outbox.domain.OutboxStatus;
import dev.iamrat.messaging.publisher.application.EventPublisher;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class OutboxRelayService {

    private final OutboxMessageStore outboxMessageStore;
    private final List<EventPublisher> eventPublishers;
    private final OutboxRelayPolicy outboxRelayPolicy;
    private final OutboxRetryService outboxRetryService;
    private final TransactionTemplate transactionTemplate;

    public OutboxRelayService(
        OutboxMessageStore outboxMessageStore,
        List<EventPublisher> eventPublishers,
        OutboxRelayPolicy outboxRelayPolicy,
        OutboxRetryService outboxRetryService,
        PlatformTransactionManager transactionManager
    ) {
        this.outboxMessageStore = outboxMessageStore;
        this.eventPublishers = eventPublishers;
        this.outboxRelayPolicy = outboxRelayPolicy;
        this.outboxRetryService = outboxRetryService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int relayPending() {
        if (!outboxRelayPolicy.relayEnabled() || eventPublishers.isEmpty()) {
            return 0;
        }

        List<Long> eventIds = claimPendingEventIds();
        int publishedCount = 0;
        for (Long eventId : eventIds) {
            if (dispatch(eventId)) {
                publishedCount++;
            }
        }
        return publishedCount;
    }

    private List<Long> claimPendingEventIds() {
        return transactionTemplate.execute(status -> {
            List<OutboxMessage> messages = outboxMessageStore.findClaimableForUpdate(
                outboxRelayPolicy.batchSize(),
                outboxRelayPolicy.maxRetries()
            );
            Instant now = Instant.now();
            messages.forEach(message -> message.markProcessing(now));
            return messages.stream()
                .map(OutboxMessage::getId)
                .toList();
        });
    }

    private boolean dispatch(Long eventId) {
        OutboxMessage message = loadProcessingMessage(eventId);
        if (message == null) {
            return false;
        }

        try {
            dispatchMessage(message);
            markPublished(eventId);
            return true;
        } catch (RuntimeException e) {
            markFailed(eventId, e);
            return false;
        }
    }

    private OutboxMessage loadProcessingMessage(Long eventId) {
        return transactionTemplate.execute(status -> outboxMessageStore.findById(eventId)
            .filter(message -> message.getStatus() == OutboxStatus.PROCESSING)
            .orElse(null));
    }

    private void dispatchMessage(OutboxMessage message) {
        EventType eventType = EventType.from(message.getEventType());
        List<EventPublisher> supportedPublishers = eventPublishers.stream()
            .filter(publisher -> publisher.supports(eventType))
            .toList();

        if (supportedPublishers.isEmpty()) {
            throw new IllegalStateException("No publisher supports event type: " + message.getEventType());
        }

        for (EventPublisher publisher : supportedPublishers) {
            publisher.publish(message);
        }
    }

    private void markPublished(Long eventId) {
        transactionTemplate.executeWithoutResult(status -> outboxMessageStore.findById(eventId)
            .ifPresent(message -> message.markPublished(Instant.now())));
    }

    private void markFailed(Long eventId, RuntimeException cause) {
        transactionTemplate.executeWithoutResult(status -> outboxMessageStore.findById(eventId)
            .ifPresent(message -> outboxRetryService.scheduleRetry(message, cause, Instant.now())));
        log.warn("Outbox event dispatch failed. eventId={}", eventId, cause);
    }
}
