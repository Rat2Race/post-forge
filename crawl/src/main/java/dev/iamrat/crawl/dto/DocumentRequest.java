package dev.iamrat.crawl.dto;

import java.util.Map;

public record DocumentRequest(
        String content,
        String source,
        Map<String, String> metadata
) {
}
