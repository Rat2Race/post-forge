package dev.iamrat.file.dto;

public record FileUploadResponse(
    Long fileId,
    String savedName,
    String url
) {

}