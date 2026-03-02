package dev.iamrat.file.dto;

public record StoredFileInfo(
        String savedName,
        String relativePath,
        String fullPath
) {
}
