package dev.iamrat.board.file.application;

public interface FileStorage {

    String createUploadUrl(String objectKey, String contentType);

    String createDownloadUrl(String objectKey);
}
