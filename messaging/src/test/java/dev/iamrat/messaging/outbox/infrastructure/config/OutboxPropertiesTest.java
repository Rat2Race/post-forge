package dev.iamrat.messaging.outbox.infrastructure.config;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPropertiesTest {

    @Test
    @DisplayName("outbox relay 기본값을 보수적으로 유지한다")
    void defaults_keepRelayDisabledAndRetryPolicyConservative() {
        OutboxProperties properties = new OutboxProperties();

        assertThat(properties.relayEnabled()).isFalse();
        assertThat(properties.batchSize()).isEqualTo(50);
        assertThat(properties.maxRetries()).isEqualTo(5);
        assertThat(properties.retryBackoff()).isEqualTo(Duration.ofSeconds(30));
    }
}
