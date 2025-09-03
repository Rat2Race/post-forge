//package com.postforge.auth.users.service;
//
//import com.postforge.auth.domain.dto.JwtResponseDto;
//import com.postforge.auth.domain.dto.KakaoAuthResponse;
//import com.postforge.auth.domain.dto.TokenPair;
//import com.postforge.auth.domain.entity.Account;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.RequestEntity;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Collections;
//import java.util.Objects;
//
//@RequiredArgsConstructor
//@Service
//public class OAuthService {
//    private final UserService userService;
//    private final JWTTokenService tokenService;
//
//    @Transactional
//    public TokenPair kakaoLogin(String accessToken) {
//        String identifier = authenticateFromKakao(accessToken);
//        Account account = userService.findMemberBySocialIdentifier("kakao", identifier);
//        if(account == null) {
//            account = userService.createMemberWithSocialIdentifier("kakao", "kakao", identifier);
//        }
//        return tokenService.generateTokenPair(account.getId());
//    }
//
//    public TokenPair refreshAccessToken(String token) {
//        Long userId = tokenService.extractUserId(token);
//        return tokenService.generateTokenPair(userId);
//    }
//
//    private String authenticateFromKakao(String accessToken) {
//        RestTemplate restTemplate = new RestTemplate();
//        HttpHeaders headers = new HttpHeaders();
//        headers.put("Authorization", Collections.singletonList("Bearer " + accessToken));
//
//        ResponseEntity<KakaoAuthResponse> authResponse = restTemplate.exchange(RequestEntity
//                .post("https://kapi.kakao.com/v2/user/me")
//                .headers(headers)
//                .build(), KakaoAuthResponse.class);
//
//        // 인증 실패시 throw
//        if (!authResponse.getStatusCode().is2xxSuccessful())
//            throw new RuntimeException("알 수 없는 오류");
//
//        return Objects.requireNonNull(authResponse.getBody()).id().toString();
//    }
//
//}
