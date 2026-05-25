package dev.iamrat.messaging.outbox.application;

import dev.iamrat.messaging.outbox.domain.OutboxMessage;
import dev.iamrat.messaging.outbox.domain.OutboxStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRetryServiceTest {

    @Test
    @DisplayName("retry policy의 backoff로 실패한 outbox 메시지의 재처리 시간을 계산한다")
    void scheduleRetry_usesPolicyBackoff() {
        OutboxRelayPolicy outboxRelayPolicy = new TestOutboxRelayPolicy(
            true,
            50,
            5,
            Duration.ofMinutes(2)
        );
        OutboxRetryService retryService = new OutboxRetryService(outboxRelayPolicy);
        OutboxMessage message = OutboxMessage.pending("PostCreated", "post", "1", "{\"postId\":1}");
        Instant failedAt = Instant.parse("2026-05-25T01:00:00Z");

        retryService.scheduleRetry(message, new IllegalStateException("broker unavailable"), failedAt);

        assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(message.getRetryCount()).isEqualTo(1);
        assertThat(message.getAvailableAt()).isEqualTo(failedAt.plus(Duration.ofMinutes(2)));
        assertThat(message.getLastError()).isEqualTo("broker unavailable");
    }
}
