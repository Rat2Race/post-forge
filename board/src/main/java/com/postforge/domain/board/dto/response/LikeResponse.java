package com.postforge.domain.board.dto.response;

public record LikeResponse(
    boolean isLiked,
    long likeCount
) {
}
