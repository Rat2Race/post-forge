package dev.iamrat.ingest.pipeline.application;

import dev.iamrat.ingest.pipeline.domain.DocumentChunk;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentChunker {

    public List<DocumentChunk> toChunks(List<DocumentIngestCommand> commands) {
        return commands.stream()
            .map(this::toChunk)
            .toList();
    }

    private DocumentChunk toChunk(DocumentIngestCommand command) {
        return DocumentChunk.of(command.content(), command.source(), command.metadata());
    }
}
