package dev.iamrat.source.support.error;

public final class SourceExceptionMessages {

    public static final String NEWS_TITLE_MUST_NOT_BE_BLANK = "뉴스 제목은 비어 있을 수 없습니다";
    public static final String NEWS_LINK_MUST_NOT_BE_BLANK = "뉴스 링크는 비어 있을 수 없습니다";
    public static final String NEWS_KEYWORD_MUST_NOT_BE_BLANK = "뉴스 키워드는 비어 있을 수 없습니다";
    public static final String NEWS_SORT_MUST_BE_SUPPORTED = "뉴스 정렬은 date 또는 sim이어야 합니다";

    private SourceExceptionMessages() {
    }

    public static String googleNewsSourceDisabled() {
        return "Google News RSS 소스를 사용하려면 GOOGLE_NEWS_ENABLED=true 설정이 필요합니다";
    }

    public static String googleNewsFeedUnreadable() {
        return "Google News RSS 피드를 해석할 수 없습니다";
    }
}
