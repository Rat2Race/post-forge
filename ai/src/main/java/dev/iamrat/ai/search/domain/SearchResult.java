package dev.iamrat.ai.search.domain;

import java.util.Map;

public record SearchResult(
    String text,
    Map<String, Object> metadata
) {
}
