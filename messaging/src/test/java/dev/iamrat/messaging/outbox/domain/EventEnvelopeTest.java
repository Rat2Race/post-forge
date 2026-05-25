package dev.iamrat.messaging.outbox.domain;

import dev.iamrat.core.event.EventType;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventEnvelopeTest {

    @Test
    @DisplayName("from은 문자열 event type을 core EventType으로 정규화한다")
    void fromString_normalizesEventType() {
        EventEnvelope envelope = EventEnvelope.from(
            "PostCreated",
            "post",
            "1",
            Map.of("postId", 1L)
        );

        assertThat(envelope.eventType()).isEqualTo(EventType.from("PostCreated"));
        assertThat(envelope.aggregateType()).isEqualTo("post");
        assertThat(envelope.aggregateId()).isEqualTo("1");
        assertThat(envelope.payload()).isEqualTo(Map.of("postId", 1L));
    }

    @Test
    @DisplayName("from은 typed event type을 그대로 보존한다")
    void fromTypedEventType_keepsEventType() {
        EventType eventType = EventType.from("PostDeleted");

        EventEnvelope envelope = EventEnvelope.from(
            eventType,
            "post",
            "1",
            Map.of("postId", 1L)
        );

        assertThat(envelope.eventType()).isSameAs(eventType);
        assertThat(envelope.aggregateType()).isEqualTo("post");
        assertThat(envelope.aggregateId()).isEqualTo("1");
        assertThat(envelope.payload()).isEqualTo(Map.of("postId", 1L));
    }

    @Test
    @DisplayName("typed event type을 그대로 보존한다")
    void typedConstructor_keepsEventType() {
        EventType eventType = EventType.from("PostDeleted");

        EventEnvelope envelope = new EventEnvelope(
            eventType,
            "post",
            "1",
            Map.of("postId", 1L)
        );

        assertThat(envelope.eventType()).isSameAs(eventType);
    }

    @Test
    @DisplayName("payload는 null일 수 없다")
    void constructor_nullPayload_throwsException() {
        assertThatThrownBy(() -> new EventEnvelope(
            EventType.from("PostCreated"),
            "post",
            "1",
            null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("payload must not be null");
    }
}
