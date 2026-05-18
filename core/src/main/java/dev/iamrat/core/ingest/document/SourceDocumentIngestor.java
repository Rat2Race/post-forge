package dev.iamrat.core.ingest.document;

import java.util.List;

public interface SourceDocumentIngestor {
    int ingest(List<SourceDocumentCommand> commands);
}
