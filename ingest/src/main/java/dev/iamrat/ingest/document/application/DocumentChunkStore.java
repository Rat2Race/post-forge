package dev.iamrat.ingest.document.application;

import java.util.List;
import org.springframework.ai.document.Document;

public interface DocumentChunkStore {

    void store(List<Document> chunks);
}
