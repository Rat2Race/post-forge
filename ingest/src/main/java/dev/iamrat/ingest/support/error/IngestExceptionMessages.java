package dev.iamrat.ingest.support.error;

public final class IngestExceptionMessages {

    public static final String NEWS_KEYWORD_MUST_NOT_BE_BLANK = "뉴스 키워드는 비어 있을 수 없습니다";
    public static final String LAUNCH_NEWS_KEYWORD_MUST_NOT_BE_BLANK =
        "출시 뉴스 키워드는 비어 있을 수 없습니다";
    public static final String RAW_PRODUCT_PAYLOAD_MUST_BE_JSON_SERIALIZABLE =
        "원본 상품 페이로드는 JSON으로 직렬화할 수 있어야 합니다";

    private IngestExceptionMessages() {
    }
}
