package dev.iamrat.board.like.dto;

public record LikeResponse(
    boolean isLiked,
    long likeCount
) {
}
