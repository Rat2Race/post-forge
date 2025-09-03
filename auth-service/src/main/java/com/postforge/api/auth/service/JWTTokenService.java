//package com.postforge.auth.users.service;
//
//
//import com.postforge.auth.domain.dto.TokenPair;
//import com.postforge.auth.security.config.TokenProperties;
//import io.jsonwebtoken.*;
//import org.springframework.stereotype.Component;
//
//import javax.crypto.spec.SecretKeySpec;
//import java.security.Key;
//import java.util.Date;
//import java.util.Map;
//
//@Component
//public class JWTTokenService {
//    private static final String USER_ID_KEY_NAME = "userId";
//    private final TokenProperties tokenProperties;
//    private final Key signKey;
//
//    public JWTTokenService(TokenProperties tokenProperties) {
//        this.tokenProperties = tokenProperties;
//
//        byte[] secretKeyArray = tokenProperties.secretKey().getBytes();
//        this.signKey = new SecretKeySpec(secretKeyArray, SignatureAlgorithm.HS256.getJcaName());
//    }
//
//    public TokenPair generateTokenPair(Long userId) {
//        String accessToken = generateAccessToken(userId);
//        String refreshToken = generateRefreshToken(userId);
//        return new TokenPair(accessToken, refreshToken);
//    }
//
//    public Long extractUserId(String token) {
//        try {
//            Jws<Claims> tokenClaim = Jwts.parserBuilder().setSigningKey(this.signKey).build()
//                    .parseClaimsJws(token);
//            return tokenClaim.getBody().get(USER_ID_KEY_NAME, Long.class);
//        }catch (Exception e) {
//            throw new RuntimeException();
//        }
//    }
//
//    private Date generateAccessTokenExpiration() {
//        return new Date(System.currentTimeMillis() + Long.parseLong(tokenProperties.expiration().accessToken()));
//    }
//
//    private Date generateRefreshTokenExpiration() {
//        return new Date(System.currentTimeMillis() + Long.parseLong(tokenProperties.expiration().refreshToken()));
//    }
//
//    private String generateAccessToken(Long userId) {
//        return Jwts.builder()
//                .setClaims(Map.of(USER_ID_KEY_NAME, userId))
//                .setExpiration(generateAccessTokenExpiration())
//                .signWith(signKey, SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    private String generateRefreshToken(Long userId) {
//        return Jwts.builder()
//                .setClaims(Map.of(USER_ID_KEY_NAME, userId))
//                .setExpiration(generateRefreshTokenExpiration())
//                .signWith(signKey, SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    private Map<String, Object> createTokenHeader() {
//        return Map.of(
//                "typ", "JWT",
//                "alg", "HS256",
//                "regDate", System.currentTimeMillis()
//        );
//    }
//}
