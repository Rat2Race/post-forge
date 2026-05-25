package dev.iamrat.ingest.pipeline.presentation.dto;

import dev.iamrat.ingest.pipeline.application.DocumentIngestCommand;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record DocumentRequest(
    @NotBlank(message = "내용을 입력해주세요.")
    String content,
    String source,
    Map<String, String> metadata
) {
    public DocumentIngestCommand toCommand() {
        return new DocumentIngestCommand(content, source, metadata);
    }
}
