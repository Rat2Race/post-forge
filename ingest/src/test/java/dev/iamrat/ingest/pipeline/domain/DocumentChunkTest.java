package dev.iamrat.ingest.pipeline.domain;

import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkTest {

    @Test
    void of_addsSourceAndMetadata() {
        DocumentChunk chunk = DocumentChunk.of(
            "news content",
            NewsDocumentMetadata.SOURCE_NAVER_NEWS,
            Map.of("keyword", "AI", "originalLink", "https://news.example/1")
        );

        assertThat(chunk.content()).isEqualTo("news content");
        assertThat(chunk.metadata())
            .containsEntry(SourceDocumentCommand.SOURCE_METADATA_KEY, NewsDocumentMetadata.SOURCE_NAVER_NEWS)
            .containsEntry("keyword", "AI")
            .containsEntry("originalLink", "https://news.example/1");
    }

    @Test
    void of_nullSource_omitsSource() {
        DocumentChunk chunk = DocumentChunk.of("manual content", null, Map.of("kind", "manual"));

        assertThat(chunk.metadata())
            .doesNotContainKey(SourceDocumentCommand.SOURCE_METADATA_KEY)
            .containsEntry("kind", "manual");
    }

    @Test
    void of_nullMetadata_usesOnlySource() {
        DocumentChunk chunk = DocumentChunk.of("news content", NewsDocumentMetadata.SOURCE_NAVER_NEWS, null);

        assertThat(chunk.metadata())
            .containsOnly(Map.entry(SourceDocumentCommand.SOURCE_METADATA_KEY, NewsDocumentMetadata.SOURCE_NAVER_NEWS));
    }
}
