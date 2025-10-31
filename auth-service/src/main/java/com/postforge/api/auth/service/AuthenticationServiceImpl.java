package com.postforge.api.auth.service;

import com.postforge.api.auth.dto.LoginRequest;
import com.postforge.api.auth.dto.RegisterRequest;
import com.postforge.api.auth.dto.TokenInfo;
import com.postforge.domain.member.dto.request.CommonLoginRequest;
import com.postforge.domain.member.dto.request.CommonRegisterRequest;
import com.postforge.global.security.dto.TokenResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 일단 CommonAuthService를 사용해서 구현 (테스트 과정)
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthenticationServiceImpl implements com.postforge.api.auth.AuthenticationService {

    private final CommonAuthService commonAuthService;

    @Override
    public Long register(RegisterRequest request) {
        return commonAuthService.register(CommonRegisterRequest.from(request));
    }

    @Override
    public TokenInfo login(LoginRequest request) {
        return TokenResponse.toTokenInfo(commonAuthService.login(CommonLoginRequest.from(request)));
    }

    @Override
    public TokenInfo reissueToken(String refreshToken) {
        return TokenResponse.toTokenInfo(commonAuthService.reissueToken(refreshToken));
    }

    @Override
    public void logout(String username) {
        commonAuthService.logout(username);
    }
}
