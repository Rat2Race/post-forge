package dev.iamrat.board.file.dto;

public record FileUploadResponse(
    Long fileId,
    String savedName,
    String url
) {

}