package dev.iamrat.messaging.support.error;

public final class MessagingExceptionMessages {

    public static final String EVENT_TYPE_MUST_NOT_BE_NULL = "이벤트 타입은 null일 수 없습니다";
    public static final String PAYLOAD_MUST_NOT_BE_NULL = "페이로드는 null일 수 없습니다";
    public static final String OUTBOX_PAYLOAD_MUST_BE_JSON_SERIALIZABLE = "아웃박스 페이로드는 JSON으로 직렬화할 수 있어야 합니다";

    private MessagingExceptionMessages() {
    }

    public static String noPublisherSupportsEventType(String eventType) {
        return "이벤트 타입을 지원하는 퍼블리셔가 없습니다: " + eventType;
    }
}
