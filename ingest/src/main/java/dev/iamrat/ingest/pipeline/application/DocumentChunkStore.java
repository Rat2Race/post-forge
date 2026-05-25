package dev.iamrat.ingest.pipeline.application;

import dev.iamrat.ingest.pipeline.domain.DocumentChunk;
import java.util.List;

public interface DocumentChunkStore {

    void store(List<DocumentChunk> chunks);
}
