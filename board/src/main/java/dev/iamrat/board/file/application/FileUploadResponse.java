package dev.iamrat.board.file.application;

public record FileUploadResponse(
    Long fileId,
    String savedName,
    String url
) {

}
