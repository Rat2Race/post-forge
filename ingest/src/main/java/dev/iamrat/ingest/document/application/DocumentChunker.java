package dev.iamrat.ingest.document.application;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

@Component
public class DocumentChunker {

    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
        .withMinChunkLengthToEmbed(0)
        .build();

    public List<Document> toChunks(List<SourceDocumentCommand> commands) {
        return commands.stream()
            .map(this::toDocument)
            .flatMap(document -> withStableIds(splitter.apply(List.of(document))).stream())
            .toList();
    }

    private List<Document> withStableIds(List<Document> chunks) {
        return IntStream.range(0, chunks.size())
            .mapToObj(index -> {
                Document chunk = chunks.get(index);
                String seed = index + ":" + chunk.getText();
                String id = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
                return new Document(id, chunk.getText(), chunk.getMetadata());
            })
            .toList();
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
