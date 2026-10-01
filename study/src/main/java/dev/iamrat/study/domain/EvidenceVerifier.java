package dev.iamrat.study.domain;

/**
 * 질문의 근거가 자료에 "그대로" 있는지 결정적으로 확인한다.
 * LLM이 만든 질문도, 사용자가 만든 질문도 이 검사를 통과해야 복습 목록에 들어간다.
 */
public final class EvidenceVerifier {

    private static final int MIN_QUOTE_LENGTH = 8;

    private EvidenceVerifier() {
    }

    public static boolean isQuoted(String content, String quote) {
        if (content == null || quote == null) {
            return false;
        }
        String normalizedQuote = normalize(quote);
        return normalizedQuote.length() >= MIN_QUOTE_LENGTH && normalize(content).contains(normalizedQuote);
    }

    private static String normalize(String text) {
        return text.strip().replaceAll("\\s+", " ");
    }
}
