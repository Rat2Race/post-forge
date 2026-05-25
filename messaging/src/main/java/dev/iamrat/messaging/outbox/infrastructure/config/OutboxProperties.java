package dev.iamrat.messaging.outbox.infrastructure.config;

import dev.iamrat.messaging.outbox.application.OutboxRelayPolicy;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "postforge.messaging.outbox")
public class OutboxProperties implements OutboxRelayPolicy {

    private boolean relayEnabled = false;
    private int batchSize = 50;
    private int maxRetries = 5;
    private Duration retryBackoff = Duration.ofSeconds(30);

    @Override
    public boolean relayEnabled() {
        return relayEnabled;
    }

    @Override
    public int batchSize() {
        return batchSize;
    }

    @Override
    public int maxRetries() {
        return maxRetries;
    }

    @Override
    public Duration retryBackoff() {
        return retryBackoff;
    }
}
