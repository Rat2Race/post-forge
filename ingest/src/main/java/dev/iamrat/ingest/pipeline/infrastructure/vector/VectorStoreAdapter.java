package dev.iamrat.ingest.pipeline.infrastructure.vector;

import dev.iamrat.ingest.pipeline.application.DocumentChunkStore;
import dev.iamrat.ingest.pipeline.domain.DocumentChunk;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VectorStoreAdapter implements DocumentChunkStore {

    private final VectorStore vectorStore;

    @Override
    public void store(List<DocumentChunk> chunks) {
        vectorStore.add(chunks.stream()
            .map(chunk -> new Document(chunk.content(), chunk.metadata()))
            .toList());
    }
}
