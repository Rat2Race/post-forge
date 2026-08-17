package dev.iamrat.ingest.news.application;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LaunchNewsEligibilityPolicy {

    private static final List<String> LAUNCH_KEYWORDS = List.of(
        "신제품",
        "출시",
        "공개",
        "사전예약",
        "런칭",
        "발표",
        "release",
        "launch"
    );
    private static final List<String> AD_MARKERS = List.of(
        "광고",
        "홍보",
        "스폰서",
        "협찬",
        "특가",
        "쿠폰",
        "할인",
        "이벤트",
        "최저가",
        "공동구매"
    );
    private static final List<String> UNTRUSTED_HOST_MARKERS = List.of(
        "blog.",
        "cafe.",
        "smartstore",
        "shopping",
        "ad."
    );

    public Optional<LaunchNewsSkipReason> evaluate(LaunchNewsCandidate candidate) {
        String searchableText = normalize(candidate.title() + " " + candidate.description());
        if (containsAny(searchableText, AD_MARKERS)) {
            return Optional.of(LaunchNewsSkipReason.ADVERTISING);
        }
        if (!isTrustedSource(candidate.sourceName())) {
            return Optional.of(LaunchNewsSkipReason.UNKNOWN_SOURCE);
        }
        if (!searchableText.contains(normalize(candidate.keyword()))
            || !containsAny(searchableText, LAUNCH_KEYWORDS)) {
            return Optional.of(LaunchNewsSkipReason.MISSING_LAUNCH_KEYWORD);
        }
        return Optional.empty();
    }

    private boolean isTrustedSource(String sourceName) {
        String normalized = normalize(sourceName);
        if (normalized.isBlank() || !normalized.contains(".")) {
            return false;
        }
        return UNTRUSTED_HOST_MARKERS.stream().noneMatch(normalized::contains);
    }

    private boolean containsAny(String text, List<String> needles) {
        return needles.stream()
            .map(this::normalize)
            .anyMatch(text::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
