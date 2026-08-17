package dev.iamrat.ingest.document.infrastructure.vector;

import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.ingest.document.application.DocumentChunkStore;
import dev.iamrat.ingest.support.error.IngestErrorCode;
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
    public void store(List<Document> chunks) {
        try {
            vectorStore.add(chunks);
            counter("ingest_documents_embeddings_stored_total").increment(chunks.size());
        } catch (RuntimeException exception) {
            counter("ingest_documents_embeddings_failed_total").increment(chunks.size());
            log.error("Vector document store unavailable", exception);
            throw new CustomException(IngestErrorCode.DOCUMENT_STORE_UNAVAILABLE);
        }
    }

    private Counter counter(String name) {
        return Counter.builder(name).register(meterRegistry);
    }
}
