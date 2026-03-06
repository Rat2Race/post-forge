package dev.iamrat.file.dto;

import dev.iamrat.file.entity.PostFile;

public record FileInfoResponse(
    Long fileId,
    String originalFileName,
    String fileType
) {
    public static FileInfoResponse from(PostFile file) {
        return new FileInfoResponse(
            file.getId(),
            file.getOriginalFileName(),
            file.getFileType()
        );
    }
}
