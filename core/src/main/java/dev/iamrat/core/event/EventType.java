package dev.iamrat.core.event;

public record EventType(String value) {

    public EventType {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
    }

    public static EventType from(String value) {
        return new EventType(value);
    }
}
