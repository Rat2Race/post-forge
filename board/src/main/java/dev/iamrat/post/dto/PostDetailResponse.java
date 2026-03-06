package dev.iamrat.post.dto;

import dev.iamrat.file.dto.FileInfoResponse;
import dev.iamrat.post.entity.Post;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
    Long id,
    String title,
    String content,
    String userId,
    Long views,
    Integer commentCount,
    Long likeCount,
    boolean isLiked,
    List<FileInfoResponse> files,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static PostDetailResponse from(Post post, boolean isLiked, Long likeCount) {
        List<FileInfoResponse> files = post.getFiles().stream()
            .map(FileInfoResponse::from)
            .toList();

        return new PostDetailResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getUserId(),
            post.getViews(),
            post.getComments().size(),
            likeCount,
            isLiked,
            files,
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}
