package dev.iamrat.board.post.application;

import dev.iamrat.board.file.domain.PostFile;

public record PostFileResult(
    Long fileId,
    String originalFileName,
    String fileType
) {
    public static PostFileResult from(PostFile file) {
        return new PostFileResult(
            file.getId(),
            file.getOriginalFileName(),
            file.getFileType()
        );
    }
}
