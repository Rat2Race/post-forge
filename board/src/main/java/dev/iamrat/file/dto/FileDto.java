package dev.iamrat.file.dto;

import org.springframework.core.io.Resource;

public record FileDto(
        Resource resource,
        String originalName,
        String mimeType
) {
}
