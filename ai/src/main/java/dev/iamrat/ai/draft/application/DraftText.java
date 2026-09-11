package dev.iamrat.ai.draft.application;

final class DraftText {

    static final int MAX_CONTENT_LENGTH = 10_000;

    private DraftText() {
    }

    static String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        int end = Character.isHighSurrogate(value.charAt(maxLength - 1))
            && Character.isLowSurrogate(value.charAt(maxLength))
            ? maxLength - 1
            : maxLength;
        return value.substring(0, end);
    }
}
