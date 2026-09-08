package dev.iamrat.source.support.error;

public final class SourceExceptionMessages {

    public static final String NEWS_TITLE_MUST_NOT_BE_BLANK = "뉴스 제목은 비어 있을 수 없습니다";
    public static final String NEWS_LINK_MUST_NOT_BE_BLANK = "뉴스 링크는 비어 있을 수 없습니다";
    public static final String NEWS_KEYWORD_MUST_NOT_BE_BLANK = "뉴스 키워드는 비어 있을 수 없습니다";
    public static final String NEWS_SORT_MUST_BE_SUPPORTED = "뉴스 정렬은 date 또는 sim이어야 합니다";

    private static final String NAVER_API_HUB_CREDENTIALS =
        "NAVER_API_HUB_API_KEY_ID/NAVER_API_HUB_API_KEY 설정이 필요합니다";

    private SourceExceptionMessages() {
    }

    public static String naverNewsSourceRequiresCredentials() {
        return "Naver News 소스를 사용하려면 NAVER_NEWS_ENABLED=true와 " + NAVER_API_HUB_CREDENTIALS;
    }
}
