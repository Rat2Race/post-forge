package dev.iamrat.board.like.application;

public record LikeResponse(
    boolean isLiked,
    long likeCount
) {
}
