package dev.iamrat.messaging.outbox.application;

import java.time.Duration;

record TestOutboxRelayPolicy(
    boolean relayEnabled,
    int batchSize,
    int maxRetries,
    Duration retryBackoff
) implements OutboxRelayPolicy {

    static TestOutboxRelayPolicy enabled() {
        return new TestOutboxRelayPolicy(true, 50, 5, Duration.ofSeconds(30));
    }

    static TestOutboxRelayPolicy disabled() {
        return new TestOutboxRelayPolicy(false, 50, 5, Duration.ofSeconds(30));
    }
}
