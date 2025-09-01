//package com.postforge.auth.users.controller;
//
//import com.postforge.auth.domain.dto.JwtResponseDto;
//import com.postforge.auth.domain.dto.KakaoLoginRequestDto;
//import com.postforge.auth.domain.dto.RefreshTokenRequestDto;
//import com.postforge.auth.domain.dto.TokenPair;
//import com.postforge.auth.users.service.OAuthService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequiredArgsConstructor
//@Tag(name = "OAuth", description = "OAuth 인증 관련 API")
//@RequestMapping("/v1/oauth")
//public class OAuthController {
//    private final OAuthService oAuthService;
//
//    @Operation(summary = "카카오 로그인")
//    @PostMapping("/kakao")
//    ResponseEntity<JwtResponseDto> loginWithKakao(
//            @RequestBody KakaoLoginRequestDto request
//    ) {
//        TokenPair tokens = oAuthService.kakaoLogin(request.accessToken());
//        return ResponseEntity.ok(new JwtResponseDto(tokens.accessToken(), tokens.refreshToken()));
//    }
//
//    @Operation(summary = "토큰 갱신")
//    @PostMapping("/refresh")
//    ResponseEntity<JwtResponseDto> refreshToken(
//            @RequestBody RefreshTokenRequestDto request
//    ) {
//        TokenPair tokens = oAuthService.refreshAccessToken(request.refreshToken());
//        return ResponseEntity.ok(new JwtResponseDto(tokens.accessToken(), tokens.refreshToken()));
//    }
//}
//
