package dev.iamrat.board.post.presentation.dto;

import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.post.application.PostFileResult;

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

    public static FileInfoResponse from(PostFileResult result) {
        return new FileInfoResponse(
            result.fileId(),
            result.originalFileName(),
            result.fileType()
        );
    }
}
