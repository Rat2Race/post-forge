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

    private static final String NAVER_SEARCH_CREDENTIALS =
        "NAVER_SEARCH_CLIENT_ID/NAVER_SEARCH_CLIENT_SECRET 설정이 필요합니다";

    private SourceExceptionMessages() {
    }

    public static String naverNewsSourceRequiresCredentials() {
        return "Naver News 소스를 사용하려면 NAVER_NEWS_ENABLED=true와 " + NAVER_SEARCH_CREDENTIALS;
    }

    public static String naverShoppingSourceRequiresCredentials() {
        return "Naver Shopping 소스를 사용하려면 NAVER_SHOPPING_ENABLED=true와 " + NAVER_SEARCH_CREDENTIALS;
    }
}
