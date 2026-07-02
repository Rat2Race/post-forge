package dev.iamrat.messaging.outbox.infrastructure.scheduler;

import dev.iamrat.messaging.outbox.application.OutboxRelayService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "postforge.messaging.outbox",
    name = "relay-enabled",
    havingValue = "true"
)
public class OutboxRelayScheduler {

    private final OutboxRelayService outboxRelayService;

    @Scheduled(fixedDelayString = "${postforge.messaging.outbox.relay-interval-ms:10000}")
    public void relayPending() {
        outboxRelayService.relayPending();
    }
}
