package dev.iamrat.file.support;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
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
            throw new CustomException(ErrorCode.FILE_EXTENSION_NOT_ALLOWED);
        }

        return extension;
    }

    public String validateDeclaredContentType(String fileName, String contentType) {
        String extension = extractAllowedExtension(fileName);
        return validateExpectedMimeType(extension, contentType);
    }

    private String validateExpectedMimeType(String extension, String actualMimeType) {
        String normalizedMimeType = actualMimeType == null ? "" : actualMimeType.trim().toLowerCase();
        String expectedMimeType = EXTENSION_MIME_MAP.get(extension);

        if (!expectedMimeType.equals(normalizedMimeType)) {
            throw new CustomException(ErrorCode.FILE_TYPE_MISMATCH);
        }

        return expectedMimeType;
    }
}
