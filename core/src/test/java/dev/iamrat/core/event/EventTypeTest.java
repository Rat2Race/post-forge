package dev.iamrat.core.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTypeTest {

    @Test
    @DisplayName("event type value를 보존한다")
    void from_validValue_keepsValue() {
        EventType eventType = EventType.from("PostCreated");

        assertThat(eventType.value()).isEqualTo("PostCreated");
    }

    @Test
    @DisplayName("blank event type은 허용하지 않는다")
    void from_blankValue_throwsException() {
        assertThatThrownBy(() -> EventType.from(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("eventType must not be blank");
    }
}
