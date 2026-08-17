package dev.iamrat.ingest.document.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class DocumentChunkerTest {

    @Test
    @DisplayName("짧은 유효 문서도 버리지 않는다")
    void toChunks_shortDocument_keepsContent() {
        SourceDocumentCommand command = new SourceDocumentCommand("a", "manual", Map.of());

        List<Document> chunks = new DocumentChunker().toChunks(List.of(command));

        assertThat(chunks).singleElement().extracting(Document::getText).isEqualTo("a");
    }

    @Test
    @DisplayName("긴 문서를 여러 token chunk로 분할하고 metadata를 유지한다")
    void toChunks_longDocument_splitsAndKeepsMetadata() {
        SourceDocumentCommand command = new SourceDocumentCommand(
            "PostForge 문서 분할 테스트입니다. ".repeat(1_000),
            "manual",
            Map.of("keyword", "tech")
        );

        List<Document> chunks = new DocumentChunker().toChunks(List.of(command));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.getMetadata())
            .containsEntry(SourceDocumentCommand.SOURCE_METADATA_KEY, "manual")
            .containsEntry("keyword", "tech"));
    }
}
