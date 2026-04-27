package dev.iamrat.common.dto;

import java.util.Map;

public record InternalDocumentPayload(
        String content,
        String source,
        Map<String, String> metadata
) {
}

