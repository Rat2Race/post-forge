package dev.iamrat.core.event;

public record EventType(String value) {

    private static final String EVENT_TYPE_MUST_NOT_BE_BLANK = "이벤트 타입은 비어 있을 수 없습니다";

    public EventType {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(EVENT_TYPE_MUST_NOT_BE_BLANK);
        }
    }

    public static EventType from(String value) {
        return new EventType(value);
    }
}
