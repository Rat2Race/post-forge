package dev.iamrat.board.post.dto;

import dev.iamrat.board.file.domain.PostFile;

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
