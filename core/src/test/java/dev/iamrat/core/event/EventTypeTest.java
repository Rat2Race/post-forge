package dev.iamrat.core.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTypeTest {

    @Test
    @DisplayName("이벤트 타입 값을 보존한다")
    void from_validValue_keepsValue() {
        EventType eventType = EventType.from("PostCreated");

        assertThat(eventType.value()).isEqualTo("PostCreated");
    }

    @Test
    @DisplayName("빈 이벤트 타입은 허용하지 않는다")
    void from_blankValue_throwsException() {
        assertThatThrownBy(() -> EventType.from(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이벤트 타입은 비어 있을 수 없습니다");
    }
}
