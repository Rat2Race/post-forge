package com.springweb.study.security.jwt;

import io.jsonwebtoken.*;

import java.security.Key;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javax.crypto.SecretKey;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@PropertySource("classpath:application-jwt.properties")
@Component
@Slf4j
public class JwtUtils {
	private final SecretKey secretKey;
	private final long expirationHours;
	private final String issuer;

	public JwtUtils(
			@Value("${secret-key}") String secretKey,
			@Value("${expiration-hours}") long expirationHours,
			@Value("${issuer}") String issuer
	) {
		this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
		this.expirationHours = expirationHours;
		this.issuer = issuer;
	}

	public String createToken(String subject) {
		return Jwts.builder()
				.signWith(secretKey)
				.subject(subject)
				.issuer(issuer)
				.issuedAt(Timestamp.valueOf(LocalDateTime.now()))
				.expiration(Date.from(Instant.now().plus(expirationHours, ChronoUnit.HOURS)))
				.compact();
	}

	public String validateTokenAndGetSubject(String jws) {
		try {
			return Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(jws)
					.getPayload()
					.getSubject();
		} catch (MalformedJwtException e) {
			log.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			log.error("JWT token is expired: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			log.error("JWT token is unsupported: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			log.error("JWT claims string is empty: {}", e.getMessage());
		}

		return null;
	}
}
