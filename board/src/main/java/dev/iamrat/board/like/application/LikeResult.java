package dev.iamrat.board.like.application;

public record LikeResult(
    boolean isLiked,
    long likeCount
) {
}
