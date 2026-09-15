package dev.iamrat.board.file.domain;

import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.exception.CustomException;

import java.util.Map;

public class FileTypePolicy {
    private static final Map<String, String> EXTENSION_MIME_MAP = Map.of(
        ".jpg", "image/jpeg",
        ".jpeg", "image/jpeg",
        ".png", "image/png",
        ".gif", "image/gif",
        ".pdf", "application/pdf"
    );

    public String extractAllowedExtension(String fileName) {
        String extension = "";

        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        }

        if (!EXTENSION_MIME_MAP.containsKey(extension)) {
            throw new CustomException(BoardErrorCode.FILE_EXTENSION_NOT_ALLOWED);
        }

        return extension;
    }

    public String validateDeclaredContentType(String fileName, String contentType) {
        String extension = extractAllowedExtension(fileName);
        return validateExpectedMimeType(extension, contentType);
    }

    public ValidatedFileType validate(String fileName, String contentType) {
        String extension = extractAllowedExtension(fileName);
        String mimeType = validateExpectedMimeType(extension, contentType);
        return new ValidatedFileType(extension, mimeType, determineFolder(mimeType));
    }

    public String determineFolder(String contentType) {
        if (contentType == null) {
            return "others";
        }
        if (contentType.startsWith("image/")) {
            return "images";
        }
        if (contentType.startsWith("video/")) {
            return "videos";
        }
        if (contentType.equals("application/pdf")) {
            return "documents";
        }
        return "others";
    }

    private String validateExpectedMimeType(String extension, String actualMimeType) {
        String normalizedMimeType = actualMimeType == null ? "" : actualMimeType.trim().toLowerCase();
        String expectedMimeType = EXTENSION_MIME_MAP.get(extension);

        if (!expectedMimeType.equals(normalizedMimeType)) {
            throw new CustomException(BoardErrorCode.FILE_TYPE_MISMATCH);
        }

        return expectedMimeType;
    }

    public record ValidatedFileType(String extension, String mimeType, String storageFolder) {
    }
}
