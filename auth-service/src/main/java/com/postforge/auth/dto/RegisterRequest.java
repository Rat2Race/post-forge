package com.postforge.auth.dto;

public record RegisterRequest(
    String name,
    String id,
    String pw
) {

}
