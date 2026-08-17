package dev.iamrat.ingest.document.application;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

@Component
public class DocumentChunker {

    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
        .withMinChunkLengthToEmbed(0)
        .build();

    public List<Document> toChunks(List<SourceDocumentCommand> commands) {
        List<Document> documents = commands.stream()
            .map(this::toDocument)
            .toList();
        return splitter.apply(documents);
    }

    private Document toDocument(SourceDocumentCommand command) {
        Map<String, Object> metadata = new HashMap<>();
        if (command.source() != null) {
            metadata.put(SourceDocumentCommand.SOURCE_METADATA_KEY, command.source());
        }
        if (command.metadata() != null) {
            metadata.putAll(command.metadata());
        }
        return new Document(command.content(), metadata);
    }
}
