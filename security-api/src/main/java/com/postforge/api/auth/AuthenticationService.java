package com.postforge.api.auth;

import com.postforge.api.auth.dto.LoginRequest;
import com.postforge.api.auth.dto.RegisterRequest;
import com.postforge.api.auth.dto.TokenInfo;

public interface AuthenticationService {
    Long register(RegisterRequest request);
    TokenInfo login(LoginRequest request);
    TokenInfo reissueToken(String refreshToken);
    void logout(String username);
    boolean validateToken(String token);
}
