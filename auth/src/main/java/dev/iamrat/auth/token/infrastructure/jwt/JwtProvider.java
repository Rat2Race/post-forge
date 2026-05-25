package dev.iamrat.auth.token.infrastructure.jwt;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.token.application.TokenClaims;
import dev.iamrat.auth.token.application.TokenIssuer;
import dev.iamrat.core.global.exception.CustomException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class JwtProvider implements TokenIssuer {
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    public String generateAccessToken(Long accountId, Collection<String> authorityNames) {
        Map<String, Object> claims = new HashMap<>();
        
        claims.put("roles", authorityNames.stream().toList());
        
        return generateToken(accountIdSubject(accountId), claims, Duration.ofMinutes(jwtProperties.getAccessTokenValidity()));
    }
    
    @Override
    public String generateRefreshToken(Long accountId) {
        return generateToken(accountIdSubject(accountId), Collections.emptyMap(), Duration.ofDays(jwtProperties.getRefreshTokenValidity()));
    }

    @Override
    public TokenClaims parse(String token) {
        Claims claims = parseClaims(token);

        return new TokenClaims(claims.getSubject(), extractRoles(claims));
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (SecurityException | MalformedJwtException e) {
            log.debug("JWT 서명 검증 실패: {}", e.getMessage());
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            log.debug("JWT 만료: {}", e.getMessage());
            throw new CustomException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 검증 실패: {}", e.getMessage());
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private List<String> extractRoles(Claims claims) {
        List<?> roles = claims.get("roles", List.class);
        if (roles == null) {
            return List.of();
        }

        return roles.stream()
            .map(String::valueOf)
            .toList();
    }
    
    private String generateToken(String subject, Map<String, Object> claims, Duration validity) {
        if (subject == null || subject.isBlank()) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
        
        Instant now = Instant.now();
        Instant expiry = now.plus(validity);
        
        return Jwts.builder()
            .subject(subject)
            .claims(claims)
            .issuer("post-forge-auth")
            .audience().add("post-forge-api").and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact();
    }

    private String accountIdSubject(Long accountId) {
        if (accountId == null) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
        return String.valueOf(accountId);
    }
}
