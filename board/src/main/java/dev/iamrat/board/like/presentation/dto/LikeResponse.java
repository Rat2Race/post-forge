package dev.iamrat.board.like.presentation.dto;

import dev.iamrat.board.like.application.LikeResult;

public record LikeResponse(
    boolean isLiked,
    long likeCount
) {
    public static LikeResponse from(LikeResult result) {
        return new LikeResponse(result.isLiked(), result.likeCount());
    }
}
