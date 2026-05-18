package dev.iamrat.auth.login.service;

import dev.iamrat.core.global.security.UserPrincipal;
import dev.iamrat.auth.login.dto.LoginRequest;
import dev.iamrat.auth.login.support.LoginAttemptGuard;
import dev.iamrat.auth.token.dto.JwtResponse;
import dev.iamrat.auth.token.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final LoginAttemptGuard loginAttemptGuard;
    
    public JwtResponse login(LoginRequest request, String clientIp) {
        loginAttemptGuard.guard(request.userId(), clientIp);

        UsernamePasswordAuthenticationToken authenticationToken =
            UsernamePasswordAuthenticationToken.unauthenticated(request.userId(), request.password());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (AuthenticationException e) {
            loginAttemptGuard.recordFailure(request.userId());
            throw e;
        }

        loginAttemptGuard.clearFailure(request.userId());
        log.info("사용자 로그인: {}", authentication.getName());
        
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        
        return jwtProvider.createToken(authentication.getName(), userDetails.getNickname(), authentication.getAuthorities());
    }
    
    public void logout(String userId) {
        jwtProvider.deleteToken(userId);
        log.info("사용자 로그아웃: {}", userId);
    }
}
