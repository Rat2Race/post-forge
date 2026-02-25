package dev.iamrat.login.service;

import dev.iamrat.login.dto.LoginRequest;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.provider.JwtProvider;
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
    private final JwtProvider jwtProvider;
    
    public JwtResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
            UsernamePasswordAuthenticationToken.unauthenticated(request.id(), request.pw());
        
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        log.info("사용자 로그인: {}", authentication.getName());
        
        return jwtProvider.createToken(authentication);
    }
    
    public void logout(String userId) {
        jwtProvider.deleteToken(userId);
        log.info("사용자 로그아웃: {}", userId);
    }
}
