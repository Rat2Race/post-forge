package dev.iamrat.common.dto;

public record LikeResponse(
    boolean isLiked,
    long likeCount
) {
}
