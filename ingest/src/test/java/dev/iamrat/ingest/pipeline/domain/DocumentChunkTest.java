package dev.iamrat.ingest.pipeline.domain;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkTest {

    @Test
    @DisplayName("문서 청크 생성 시 source와 metadata를 추가한다")
    void of_addsSourceAndMetadata() {
        DocumentChunk chunk = DocumentChunk.of(
            "manual content",
            "manual",
            Map.of("keyword", "tech")
        );

        assertThat(chunk.content()).isEqualTo("manual content");
        assertThat(chunk.metadata())
            .containsEntry(SourceDocumentCommand.SOURCE_METADATA_KEY, "manual")
            .containsEntry("keyword", "tech");
    }

    @Test
    @DisplayName("source가 null이면 metadata에서 제외한다")
    void of_nullSource_omitsSource() {
        DocumentChunk chunk = DocumentChunk.of("manual content", null, Map.of("kind", "manual"));

        assertThat(chunk.metadata())
            .doesNotContainKey(SourceDocumentCommand.SOURCE_METADATA_KEY)
            .containsEntry("kind", "manual");
    }

    @Test
    @DisplayName("metadata가 null이면 source만 사용한다")
    void of_nullMetadata_usesOnlySource() {
        DocumentChunk chunk = DocumentChunk.of("manual content", "manual", null);

        assertThat(chunk.metadata())
            .containsOnly(Map.entry(SourceDocumentCommand.SOURCE_METADATA_KEY, "manual"));
    }
}
