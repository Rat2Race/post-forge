package dev.iamrat.messaging.outbox.application;

import java.time.Duration;

public interface OutboxRelayPolicy {

    boolean relayEnabled();

    int batchSize();

    int maxRetries();

    Duration retryBackoff();
}
