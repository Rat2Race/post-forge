package dev.iamrat.token.provider;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.login.dto.CustomUserDetails;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;
    private SecretKey key;

    @PostConstruct
    protected void init() {
        String secret = jwtProperties.getSecret();
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Authentication authentication) {
        String userId = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        Map<String, Object> claims = new HashMap<>();
        
        claims.put("roles", authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

        return createToken(userId, claims,
            Duration.ofMinutes(jwtProperties.getAccessTokenValidity()), true);
    }

    public String createRefreshToken(String userId) {
        return createToken(userId, Collections.emptyMap(),
            Duration.ofDays(jwtProperties.getRefreshTokenValidity()), false);
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(key)
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

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        String userId = claims.getSubject();
        log.debug("[JWT] Claims 파싱 성공 - subject={}", userId);
        
        if (userId == null || userId.isBlank()) {
            log.warn("[JWT] subject 값이 비어있음");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        
        log.debug("[JWT] UserDetails 조회 시작 - userId={}", userId);
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
        
        log.debug("[JWT] UserDetails 조회 성공 - authorities={}",
            userDetails.getAuthorities());
        
        Authentication auth =
            UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                token,
                userDetails.getAuthorities()
            );
        
        log.debug("[JWT] Authentication 생성 완료 - principal={}",
            userDetails.getUsername());
        
        return auth;
    }

    private String createToken(
        String subject,
        Map<String, Object> claims,
        Duration validity,
        boolean includeJti
    ) {
        Instant now = Instant.now();
        Instant expiry = now.plus(validity);

        JwtBuilder builder = Jwts.builder();

        if (claims != null && !claims.isEmpty()) {
            builder.claims(claims);
        }

        builder
            .subject(subject)
            .issuer("post-forge-auth")
            .audience().add("post-forge-api").and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key);

        if (includeJti) {
            builder.id(UUID.randomUUID().toString());
        }

        return builder.compact();
    }
}
