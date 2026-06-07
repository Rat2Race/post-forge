package dev.iamrat.board.file.presentation.dto;

import dev.iamrat.board.file.application.FileUploadResult;

public record FileUploadResponse(
    Long fileId,
    String savedName,
    String url
) {
    public static FileUploadResponse from(FileUploadResult result) {
        return new FileUploadResponse(result.fileId(), result.savedName(), result.url());
    }
}
