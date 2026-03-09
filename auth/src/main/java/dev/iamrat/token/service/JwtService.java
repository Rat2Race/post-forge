package dev.iamrat.token.service;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.token.entity.RefreshToken;
import dev.iamrat.token.repository.RefreshTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtService {
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecretKey secretKey;
    
    public JwtService(JwtProperties jwtProperties, RefreshTokenRepository refreshTokenRepository) {
        this.jwtProperties = jwtProperties;
        this.refreshTokenRepository = refreshTokenRepository;
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateAccessToken(String userId, Collection<? extends GrantedAuthority> authorities) {
        Map<String, Object> claims = new HashMap<>();
        
        claims.put("roles", authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        
        return generateToken(userId, claims, Duration.ofMinutes(jwtProperties.getAccessTokenValidity()));
    }
    
    public String generateRefreshToken(String userId) {
        return generateToken(userId, Collections.emptyMap(), Duration.ofDays(jwtProperties.getRefreshTokenValidity()));
    }
    
    public RefreshToken getRefreshToken(String userId) {
        return refreshTokenRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
    }
    
    public void saveOrUpdateRefreshToken(String userId, String newRefreshToken) {
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusDays(jwtProperties.getRefreshTokenValidity());
        
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId)
                .orElse(null);
        
        if(refreshToken == null) {
            refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(newRefreshToken)
                .expiryDate(expiryDate)
                .build();
        } else {
            refreshToken.updateToken(newRefreshToken, expiryDate);
        }
        
        refreshTokenRepository.save(refreshToken);
    }
    
    @Transactional
    public void deleteRefreshToken(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
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
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            log.debug("JWT 만료: {}", e.getMessage());
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 검증 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
    
    private String generateToken(String subject, Map<String, Object> claims, Duration validity) {
        if (subject == null || subject.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
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
