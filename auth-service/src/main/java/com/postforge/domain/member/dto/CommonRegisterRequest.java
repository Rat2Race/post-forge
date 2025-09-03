package com.postforge.domain.member.dto;

public record CommonRegisterRequest(
    String name,
    String id,
    String pw
) {

}
