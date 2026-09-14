package dev.iamrat.ingest.document.presentation;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record DocumentIngestRequest(
    @NotBlank(message = "내용을 입력해주세요.")
    String content,
    String source,
    Map<String, String> metadata
) {
    public SourceDocumentCommand toCommand() {
        return new SourceDocumentCommand(content, source, metadata);
    }
}
