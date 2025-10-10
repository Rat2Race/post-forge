package com.postforge.api.auth.dto;

public record RegisterRequest(
    String name,
    String id,
    String pw
) {
}
