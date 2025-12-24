package com.postforge.common.dto;

public record LikeResponse(
    boolean isLiked,
    long likeCount
) {
}
