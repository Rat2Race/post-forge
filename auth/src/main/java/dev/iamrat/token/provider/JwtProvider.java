package dev.iamrat.token.provider;

import com.nimbusds.oauth2.sdk.TokenResponse;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.login.service.CustomUserDetailsService;
import dev.iamrat.member.entity.Member;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.entity.RefreshToken;
import dev.iamrat.token.service.JwtProperties;
import dev.iamrat.token.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtService jwtService;
    private final MemberService memberService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtResponse createToken(Authentication authentication) {
        String accessToken = jwtService.generateAccessToken(authentication.getName(), authentication.getAuthorities());
        String refreshToken = jwtService.generateRefreshToken(authentication.getName());
        
        jwtService.saveOrUpdateRefreshToken(authentication.getName(), refreshToken);
        
        return JwtResponse.builder()
            .grantType("Bearer")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }
    
    public JwtResponse reissueToken(String refreshToken) {
        String userId = jwtService.parseClaims(refreshToken).getSubject();
        
        jwtService.getRefreshToken(userId).validateToken(refreshToken);
        
        Member member = memberService.findByUserId(userId);
        
        String newAccessToken = jwtService.generateAccessToken(userId, member.getAuthorities());
        String newRefreshToken = jwtService.generateRefreshToken(userId);
        
        jwtService.saveOrUpdateRefreshToken(userId, newRefreshToken);
        
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
        String userId = jwtService.parseClaims(token).getSubject();
        log.debug("[JWT] Claims 파싱 성공 - subject={}", userId);
        
        if (userId == null || userId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userId);
        
        log.debug("[JWT] UserDetails 조회 성공 - authorities={}", userDetails.getAuthorities());
        
        return UsernamePasswordAuthenticationToken.authenticated(
            userDetails, token, userDetails.getAuthorities()
        );
    }
}
