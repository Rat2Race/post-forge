package dev.iamrat.auth.token.service;

import dev.iamrat.auth.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtService {
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties, RedisTemplate<String, String> redisTemplate) {
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateAccessToken(String userId, String nickname, Collection<? extends GrantedAuthority> authorities) {
        Map<String, Object> claims = new HashMap<>();
        
        claims.put("roles", authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        
        claims.put("nickname", nickname);
        
        return generateToken(userId, claims, Duration.ofMinutes(jwtProperties.getAccessTokenValidity()));
    }
    
    public String generateRefreshToken(String userId) {
        return generateToken(userId, Collections.emptyMap(), Duration.ofDays(jwtProperties.getRefreshTokenValidity()));
    }
    
    public void validateRefreshToken(String userId, String requestToken) {
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (storedToken == null) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
        byte[] stored = storedToken.getBytes(StandardCharsets.UTF_8);
        byte[] request = requestToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(stored, request)) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public void saveRefreshToken(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                jwtProperties.getRefreshTokenValidity(),
                TimeUnit.DAYS
        );
    }

    public void deleteRefreshToken(String userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }
    
    public Claims parseClaims(String token) {
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
}
