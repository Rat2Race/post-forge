package dev.iamrat.core.ingest.document;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record NewsDocumentMetadata(
    String keyword,
    String newsTitle,
    String originalLink,
    String publishedAt,
    boolean autoPostEligible
) {
    public static final String SOURCE_NAVER_NEWS = "naver-news";
    public static final String KEYWORD = "keyword";
    public static final String NEWS_TITLE = "newsTitle";
    public static final String ORIGINAL_LINK = "originalLink";
    public static final String PUBLISHED_AT = "publishedAt";
    public static final String AUTO_POST_ELIGIBLE = "autoPostEligible";

    public static NewsDocumentMetadata autoPostEligible(
        String keyword,
        String newsTitle,
        String originalLink,
        String publishedAt
    ) {
        return new NewsDocumentMetadata(keyword, newsTitle, originalLink, publishedAt, true);
    }

    public static Optional<NewsDocumentMetadata> from(String source, Map<String, String> metadata) {
        if (!SOURCE_NAVER_NEWS.equals(source) || metadata == null) {
            return Optional.empty();
        }
        return Optional.of(new NewsDocumentMetadata(
            metadata.get(KEYWORD),
            metadata.get(NEWS_TITLE),
            metadata.get(ORIGINAL_LINK),
            metadata.get(PUBLISHED_AT),
            Boolean.parseBoolean(metadata.get(AUTO_POST_ELIGIBLE))
        ));
    }

    public Map<String, String> toMap() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put(KEYWORD, blankToEmpty(keyword));
        metadata.put(NEWS_TITLE, blankToEmpty(newsTitle));
        metadata.put(AUTO_POST_ELIGIBLE, Boolean.toString(autoPostEligible));
        metadata.put(ORIGINAL_LINK, blankToEmpty(originalLink));
        metadata.put(PUBLISHED_AT, blankToEmpty(publishedAt));
        return Map.copyOf(metadata);
    }

    public boolean canPublishAutomatically() {
        return autoPostEligible
            && hasText(keyword)
            && hasText(newsTitle)
            && hasText(originalLink);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
