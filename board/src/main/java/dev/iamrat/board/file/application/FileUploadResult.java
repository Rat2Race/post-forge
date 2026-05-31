package dev.iamrat.board.file.application;

public record FileUploadResult(
    Long fileId,
    String savedName,
    String url
) {
}
