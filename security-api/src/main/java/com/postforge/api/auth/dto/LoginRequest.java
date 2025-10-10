package com.postforge.api.auth.dto;

public record LoginRequest(
    String id,
    String pw
) {
}
