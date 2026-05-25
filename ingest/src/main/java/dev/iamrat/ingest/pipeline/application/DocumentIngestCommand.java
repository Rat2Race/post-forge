package dev.iamrat.ingest.pipeline.application;

import java.util.Map;

public record DocumentIngestCommand(
    String content,
    String source,
    Map<String, String> metadata
) {
}
