package dev.iamrat.auth.token.provider;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.login.dto.CustomUserDetails;
import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.service.AccountService;
import dev.iamrat.auth.token.dto.JwtResponse;
import dev.iamrat.auth.token.service.JwtService;
import io.jsonwebtoken.Claims;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtService jwtService;
    private final AccountService accountService;

    public JwtResponse createToken(String userId, String nickname,
                                   Collection<? extends GrantedAuthority> authorities) {
        String accessToken = jwtService.generateAccessToken(userId, nickname, authorities);
        String refreshToken = jwtService.generateRefreshToken(userId);

        jwtService.saveRefreshToken(userId, refreshToken);

        return JwtResponse.builder()
            .grantType("Bearer")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }

    public JwtResponse reissueToken(String refreshToken) {
        String userId = jwtService.parseClaims(refreshToken).getSubject();

        jwtService.validateRefreshToken(userId, refreshToken);

        Account account = accountService.findByUserId(userId);

        String newAccessToken = jwtService.generateAccessToken(userId, account.getNickname(), account.getAuthorities());
        String newRefreshToken = jwtService.generateRefreshToken(userId);

        jwtService.saveRefreshToken(userId, newRefreshToken);
        
        return JwtResponse.builder()
            .grantType("Bearer")
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
    }
    
    public void deleteToken(String userId) {
        jwtService.deleteRefreshToken(userId);
    }
    
    public Authentication resolveAuthentication(String token) {
        Claims claims = jwtService.parseClaims(token);
        CustomUserDetails principal = buildUserDetails(claims);
 
        log.debug("[JWT] UserDetails 조회 성공 - authorities={}", principal.getAuthorities());
        
        return UsernamePasswordAuthenticationToken.authenticated(
            principal, token, principal.getAuthorities()
        );
    }
    
    private CustomUserDetails buildUserDetails(Claims claims) {
        String userId = claims.getSubject();
        
        if (userId == null || userId.isBlank()) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
        
        log.debug("[JWT] Claims 파싱 성공 - subject={}", userId);
        
        String nickname = claims.get("nickname", String.class);
        List<?> rawRoles = claims.get("roles", List.class);
        
        Collection<? extends GrantedAuthority> authorities = rawRoles != null
            ? rawRoles.stream()
                .map(role -> new SimpleGrantedAuthority(String.valueOf(role)))
                .collect(Collectors.toList())
            : Collections.emptyList();
        
        return new CustomUserDetails(userId, "", nickname, authorities);
    }
}
