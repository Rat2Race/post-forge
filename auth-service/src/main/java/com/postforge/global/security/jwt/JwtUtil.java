package com.postforge.global.security.jwt;

import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private SecretKey key;

    @PostConstruct
    protected void init() {
        String secret = jwtProperties.getSecret();
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Authentication authentication) {
        String userId = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        Map<String, Object> claims = Map.of("roles",
            authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList());

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
            log.error("JWT 서명 검증 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            log.error("JWT 만료: {}", e.getMessage());
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 검증 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        String userId = claims.getSubject();
        List<String> roles = claims.get("roles", List.class);

        if (roles == null || roles.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Collection<GrantedAuthority> authorities = roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        UserDetails principal = new User(userId, "", authorities);
        return UsernamePasswordAuthenticationToken.authenticated(principal, token, authorities);
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
