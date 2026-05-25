package dev.iamrat.ingest.pipeline.domain;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.HashMap;
import java.util.Map;

public record DocumentChunk(
    String content,
    Map<String, Object> metadata
) {

    public static DocumentChunk of(String content, String source, Map<String, ?> metadata) {
        Map<String, Object> chunkMetadata = new HashMap<>();

        if (source != null) {
            chunkMetadata.put(SourceDocumentCommand.SOURCE_METADATA_KEY, source);
        }

        if (metadata != null) {
            chunkMetadata.putAll(metadata);
        }

        return new DocumentChunk(content, chunkMetadata);
    }
}
