package dev.iamrat.crawl.common.dto;

import java.util.Map;

public record DocumentRequest(
        String content,
        String source,
        Map<String, String> metadata
) {
}
