package dev.iamrat.login.service;

import dev.iamrat.login.dto.LoginRequest;
import dev.iamrat.token.dto.TokenResponse;
import dev.iamrat.token.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {
    
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    
    public TokenResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
            UsernamePasswordAuthenticationToken.unauthenticated(request.id(), request.pw());
        
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        
        log.info("사용자 로그인: {}", authentication.getName());
        
        return tokenService.createToken(authentication);
    }
    
    public void logout(String userId) {
        tokenService.deleteRefreshToken(userId);
        
        log.info("사용자 로그아웃: {}", userId);
    }
}
