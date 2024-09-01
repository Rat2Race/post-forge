package com.springweb.study.security.jwt;

import io.jsonwebtoken.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.crypto.SecretKey;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtUtils {

	private final SecretKey secretKey;
	private final long accessTokenExpirationHours;
	private final long refreshTokenExpirationHours;
	private final String issuer;

	public JwtUtils(
			@Value("${secret-key}") String secretKey,
			@Value("${at-expiration-hours}") long accessTokenExpirationHours,
			@Value("${rt-expiration-hours}") long refreshTokenExpirationHours,
			@Value("${issuer}") String issuer
	) {
		this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
		this.accessTokenExpirationHours = accessTokenExpirationHours;
		this.refreshTokenExpirationHours = refreshTokenExpirationHours;
		this.issuer = issuer;
	}

	public String createAccessToken(String username) {
		return Jwts.builder()
				.signWith(secretKey)
				.subject(username)
				.issuer(issuer)
				.issuedAt(Timestamp.valueOf(LocalDateTime.now()))
				.expiration(setExpiration(accessTokenExpirationHours))
				.compact();
	}

	public String createRefreshToken() {
		return Jwts.builder()
				.signWith(secretKey)
				.issuer(issuer)
				.issuedAt(Timestamp.valueOf(LocalDateTime.now()))
				.expiration(setExpiration(refreshTokenExpirationHours))
				.compact();
	}

	private Date setExpiration(Long expirationHours) {
		return Date.from(Instant.now().plus(expirationHours, ChronoUnit.HOURS));
	}

	public String getUsername(String jws) {
		return getClaimsFromToken(jws).getSubject();
	}

	public boolean  isAccessTokenExpired(String jws) {
		Date expiration = getClaimsFromToken(jws).getExpiration();
		return expiration.before(new Date());
	}

	private Claims getClaimsFromToken(String jws) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(jws)
				.getPayload();
	}
}
