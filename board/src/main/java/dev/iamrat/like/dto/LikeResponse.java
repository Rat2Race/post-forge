package dev.iamrat.like.dto;

public record LikeResponse(
    boolean isLiked,
    long likeCount
) {
}
