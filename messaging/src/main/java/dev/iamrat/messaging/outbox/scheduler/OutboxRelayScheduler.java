package dev.iamrat.messaging.outbox.scheduler;

import dev.iamrat.messaging.outbox.application.OutboxRelayService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxRelayService outboxRelayService;

    @Scheduled(fixedDelayString = "${postforge.messaging.outbox.relay-interval-ms:10000}")
    public void relayPending() {
        outboxRelayService.relayPending();
    }
}
