package dev.iamrat.core.ingest.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SourceDocumentCommandTest {

    @Test
    @DisplayName("소스 문서 명령은 소스 메타데이터 키 계약을 제공한다")
    void sourceMetadataKey_isStable() {
        assertThat(SourceDocumentCommand.SOURCE_METADATA_KEY).isEqualTo("source");
    }

    @Test
    @DisplayName("소스 문서 명령은 content/source/metadata를 보존한다")
    void command_preservesDocumentFields() {
        SourceDocumentCommand command = new SourceDocumentCommand(
            "manual content",
            "manual",
            Map.of("keyword", "tech")
        );

        assertThat(command.content()).isEqualTo("manual content");
        assertThat(command.source()).isEqualTo("manual");
        assertThat(command.metadata()).containsEntry("keyword", "tech");
    }
}
