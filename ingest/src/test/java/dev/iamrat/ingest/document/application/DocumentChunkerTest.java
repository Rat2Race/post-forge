package dev.iamrat.ingest.document.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
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

    @Test
    @DisplayName("같은 내용을 다시 적재하면 같은 chunk id를 만들어 덮어쓸 수 있게 한다")
    void toChunks_sameContent_producesSameChunkIds() {
        SourceDocumentCommand command = new SourceDocumentCommand(
            "갤럭시북 신제품이 출시됐다. ".repeat(200),
            "naver-news",
            Map.of("newsUrl", "https://n.news.naver.com/article/001/1")
        );

        List<Document> first = new DocumentChunker().toChunks(List.of(command));
        List<Document> second = new DocumentChunker().toChunks(List.of(command));

        assertThat(first).hasSizeGreaterThan(1);
        assertThat(first).extracting(Document::getId)
            .isEqualTo(second.stream().map(Document::getId).toList());
    }

    @Test
    @DisplayName("chunk id는 임의값이 아니라 chunk 본문에서 유도한다")
    void toChunks_derivesIdFromChunkText() {
        List<Document> chunks = new DocumentChunker().toChunks(List.of(
            new SourceDocumentCommand("첫 번째 기사", "naver-news", Map.of())
        ));

        Document chunk = chunks.getFirst();
        assertThat(chunk.getId()).isEqualTo(
            UUID.nameUUIDFromBytes(("0:" + chunk.getText()).getBytes(StandardCharsets.UTF_8)).toString()
        );
    }

    @Test
    @DisplayName("한 문서 안에서 본문이 같은 chunk가 나와도 서로 다른 id를 갖는다")
    void toChunks_repeatedChunkTextWithinOneDocument_keepsDistinctIds() {
        SourceDocumentCommand command = new SourceDocumentCommand(
            "갤럭시북 신제품이 출시됐다. ".repeat(200),
            "naver-news",
            Map.of()
        );

        List<Document> chunks = new DocumentChunker().toChunks(List.of(command));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).extracting(Document::getId).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("metadata가 달라도 본문이 같으면 같은 id로 덮어쓴다")
    void toChunks_sameTextDifferentMetadata_sharesId() {
        List<Document> first = new DocumentChunker().toChunks(List.of(
            new SourceDocumentCommand("같은 본문", "naver-news", Map.of("keyword", "갤럭시북"))
        ));
        List<Document> second = new DocumentChunker().toChunks(List.of(
            new SourceDocumentCommand("같은 본문", "naver-news", Map.of("keyword", "아이폰"))
        ));

        assertThat(first.getFirst().getId()).isEqualTo(second.getFirst().getId());
    }
}
