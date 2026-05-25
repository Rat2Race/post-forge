package dev.iamrat.ingest.pipeline.domain;

import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class IngestPolicy {

    public Optional<NewsDocumentMetadata> autoPostMetadata(String source, Map<String, String> metadata) {
        return NewsDocumentMetadata.from(source, metadata)
            .filter(NewsDocumentMetadata::canPublishAutomatically);
    }

    public boolean reserveAutoPostOriginalLink(NewsDocumentMetadata metadata, Set<String> reservedLinks) {
        return reservedLinks.add(metadata.originalLink());
    }
}
