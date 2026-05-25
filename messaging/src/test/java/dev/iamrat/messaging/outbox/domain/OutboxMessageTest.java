package dev.iamrat.messaging.outbox.domain;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxMessageTest {

    @Test
    @DisplayName("pending 이벤트는 발행 대기 상태와 재시도 기준 시간을 가진다")
    void pending_initializesPublishableState() {
        OutboxMessage message = OutboxMessage.pending(
            "CommentCreated",
            "comment",
            "10",
            "{\"commentId\":10}"
        );

        assertThat(message.getEventId()).isNotBlank();
        assertThat(message.getEventType()).isEqualTo("CommentCreated");
        assertThat(message.getAggregateType()).isEqualTo("comment");
        assertThat(message.getAggregateId()).isEqualTo("10");
        assertThat(message.getPayload()).contains("commentId");
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.getRetryCount()).isZero();
        assertThat(message.getAvailableAt()).isNotNull();
        assertThat(message.getOccurredAt()).isNotNull();
    }

    @Test
    @DisplayName("처리 실패 시 retry count를 증가시키고 backoff 이후 재처리 가능 상태로 둔다")
    void markFailed_schedulesRetry() {
        OutboxMessage message = OutboxMessage.pending(
            "CollectedItemCreated",
            "collected_item",
            "33",
            "{\"itemId\":33}"
        );
        Instant failedAt = Instant.parse("2026-05-20T01:00:00Z");

        message.markProcessing(failedAt);
        message.markFailed("smtp timeout", failedAt, Duration.ofSeconds(30));

        assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(message.getRetryCount()).isEqualTo(1);
        assertThat(message.getAvailableAt()).isEqualTo(failedAt.plusSeconds(30));
        assertThat(message.getLastError()).isEqualTo("smtp timeout");
    }

    @Test
    @DisplayName("발행 성공 시 published 상태와 시간을 기록하고 이전 오류를 지운다")
    void markPublished_recordsSuccess() {
        OutboxMessage message = OutboxMessage.pending(
            "PostCreated",
            "post",
            "1",
            "{\"postId\":1}"
        );
        Instant failedAt = Instant.parse("2026-05-20T01:00:00Z");
        Instant publishedAt = Instant.parse("2026-05-20T01:01:00Z");

        message.markFailed("temporary failure", failedAt, Duration.ofSeconds(10));
        message.markPublished(publishedAt);

        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(message.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(message.getLastError()).isNull();
    }

    @Test
    @DisplayName("event type과 payload는 비어 있을 수 없다")
    void pending_rejectsBlankRequiredFields() {
        assertThatThrownBy(() -> OutboxMessage.pending("", "post", "1", "{}"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("eventType");

        assertThatThrownBy(() -> OutboxMessage.pending("PostCreated", "post", "1", " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("payload");
    }
}
