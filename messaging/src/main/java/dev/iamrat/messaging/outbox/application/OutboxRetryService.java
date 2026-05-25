package dev.iamrat.messaging.outbox.application;

import dev.iamrat.messaging.outbox.domain.OutboxMessage;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxRetryService {

    private final OutboxRelayPolicy outboxRelayPolicy;

    public void scheduleRetry(OutboxMessage message, RuntimeException cause, Instant now) {
        message.markFailed(cause.getMessage(), now, outboxRelayPolicy.retryBackoff());
    }
}
