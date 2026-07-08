package dev.iamrat.ai.search.application;

import dev.iamrat.ai.search.domain.SearchResult;
import java.util.List;

public record SearchOutcome(
    List<SearchResult> results,
    boolean available,
    String failureReason
) {

    public SearchOutcome {
        results = results == null ? List.of() : List.copyOf(results);
        failureReason = normalizeFailureReason(failureReason);
    }

    public static SearchOutcome success(List<SearchResult> results) {
        return new SearchOutcome(results, true, null);
    }

    public static SearchOutcome unavailable(String failureReason) {
        return new SearchOutcome(List.of(), false, failureReason);
    }

    public boolean unavailable() {
        return !available;
    }

    private static String normalizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return null;
        }
        return failureReason;
    }
}
