package dev.iamrat.source.support.error;

public final class SourceExceptionMessages {

    public static final String NEWS_TITLE_MUST_NOT_BE_BLANK = "뉴스 제목은 비어 있을 수 없습니다";
    public static final String NEWS_LINK_MUST_NOT_BE_BLANK = "뉴스 링크는 비어 있을 수 없습니다";
    public static final String NEWS_KEYWORD_MUST_NOT_BE_BLANK = "뉴스 키워드는 비어 있을 수 없습니다";
    public static final String EXTERNAL_PRODUCT_ID_MUST_NOT_BE_BLANK = "외부 상품 ID는 비어 있을 수 없습니다";
    public static final String PRODUCT_TITLE_MUST_NOT_BE_BLANK = "상품명은 비어 있을 수 없습니다";
    public static final String PRODUCT_PRICE_MUST_NOT_BE_NULL = "상품 가격은 null일 수 없습니다";
    public static final String PRODUCT_SOURCE_KEYWORD_MUST_NOT_BE_BLANK =
        "상품 소스 키워드는 비어 있을 수 없습니다";

    private static final String NAVER_API_HUB_CREDENTIALS =
        "NAVER_API_HUB_API_KEY_ID/NAVER_API_HUB_API_KEY 설정이 필요합니다";

    private SourceExceptionMessages() {
    }

    public static String naverNewsSourceRequiresCredentials() {
        return "Naver News 소스를 사용하려면 NAVER_NEWS_ENABLED=true와 " + NAVER_API_HUB_CREDENTIALS;
    }

    public static String naverShoppingSourceRetired() {
        return "Naver Shopping 검색 API는 2026-07-31 종료 대상이며 API HUB 대체가 없어 지원하지 않습니다";
    }
}
