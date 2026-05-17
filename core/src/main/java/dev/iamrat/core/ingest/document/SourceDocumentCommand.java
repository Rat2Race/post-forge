package dev.iamrat.core.ingest.document;

import java.util.Map;

public record SourceDocumentCommand(
    String content,
    String source,
    Map<String, String> metadata
) {
}
