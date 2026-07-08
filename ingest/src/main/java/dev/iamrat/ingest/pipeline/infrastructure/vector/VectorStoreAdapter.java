package dev.iamrat.ingest.pipeline.infrastructure.vector;

import dev.iamrat.ingest.pipeline.application.DocumentChunkStore;
import dev.iamrat.ingest.pipeline.application.DocumentStoreResult;
import dev.iamrat.ingest.pipeline.domain.DocumentChunk;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreAdapter implements DocumentChunkStore {

    private final VectorStore vectorStore;
    private final MeterRegistry meterRegistry;

    @Override
    public DocumentStoreResult store(List<DocumentChunk> chunks) {
        try {
            vectorStore.add(chunks.stream()
                .map(chunk -> new Document(chunk.content(), chunk.metadata()))
                .toList());
            counter("ingest_documents_embeddings_stored_total").increment(chunks.size());
            return DocumentStoreResult.stored(chunks.size());
        } catch (RuntimeException exception) {
            counter("ingest_documents_embeddings_degraded_total").increment(chunks.size());
            log.warn("Vector document store unavailable; ingest request continues without embeddings. reason={}", exception.getMessage());
            return DocumentStoreResult.acceptedWithoutEmbeddings(chunks.size(), "VECTOR_STORE_UNAVAILABLE");
        }
    }

    private Counter counter(String name) {
        return Counter.builder(name).register(meterRegistry);
    }
}
