package dev.iamrat.auth.login.application;

import dev.iamrat.auth.security.infrastructure.principal.CustomUserDetails;
import dev.iamrat.auth.login.application.LoginAttemptGuard;
import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.auth.token.application.TokenService;
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
    private final TokenService tokenService;
    private final LoginAttemptGuard loginAttemptGuard;
    
    public TokenIssueResult login(String username, String password, String clientIp) {
        loginAttemptGuard.guard(username, clientIp);

        UsernamePasswordAuthenticationToken authenticationToken =
            UsernamePasswordAuthenticationToken.unauthenticated(username, password);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (AuthenticationException e) {
            loginAttemptGuard.recordFailure(username);
            throw e;
        }

        loginAttemptGuard.clearFailure(username);
        log.info("사용자 로그인: {}", authentication.getName());
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        return tokenService.createToken(userDetails.accountId(), authentication.getAuthorities());
    }
    
    public void logout(Long accountId) {
        tokenService.deleteToken(accountId);
        log.info("사용자 로그아웃: accountId={}", accountId);
    }
}
