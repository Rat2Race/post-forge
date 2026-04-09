package dev.iamrat.token.service;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    JwtProperties jwtProperties = new JwtProperties();

    JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtProperties.setSecret("test-secret-key-test-secret-key-test-secret-key");
        jwtProperties.setAccessTokenValidity(30L);
        jwtProperties.setRefreshTokenValidity(7L);

        jwtService = new JwtService(jwtProperties, redisTemplate);
    }

    @Test
    @DisplayName("올바른 파라미터로 생성한 토큰을 파싱하면 subject와 roles가 일치한다")
    void generateToken_validParams_claimsMatch() {
        String userId = "MockUserId";
        String userNickname = "MockUserNickname";
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        String accessToken = jwtService.generateAccessToken(userId, userNickname, authorities);
        Claims claims = jwtService.parseClaims(accessToken);

        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.get("nickname")).isEqualTo(userNickname);
        assertThat(claims.get("roles", List.class))
            .isEqualTo(List.of("ROLE_USER"));
    }

    @Test
    @DisplayName("잘못된 파라미터로 토큰 생성하면 INVALID_TOKEN 예외가 발생한다")
    void generateToken_blankSubject_throwsInvalidToken() {
        assertThatThrownBy(() -> jwtService.generateAccessToken(null, null, List.of()))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("Redis에 토큰이 없으면 validateRefreshToken이 INVALID_TOKEN 예외를 던진다")
    void validateRefreshToken_notFound_throwsInvalidToken() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh_token:anonymous")).willReturn(null);

        assertThatThrownBy(() -> jwtService.validateRefreshToken("anonymous", "some-token"))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("저장된 토큰과 다르면 validateRefreshToken이 INVALID_TOKEN 예외를 던진다")
    void validateRefreshToken_mismatch_throwsInvalidToken() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh_token:testuser")).willReturn("correct-token");

        assertThatThrownBy(() -> jwtService.validateRefreshToken("testuser", "wrong-token"))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("만료된 토큰을 파싱하면 EXPIRED_TOKEN 예외가 발생한다")
    void parseClaims_expiredToken_throwsExpiredToken() {
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret("test-secret-key-test-secret-key-test-secret-key");
        expiredProperties.setAccessTokenValidity(0L);
        expiredProperties.setRefreshTokenValidity(0L);

        JwtService expiredJwtService = new JwtService(expiredProperties, redisTemplate);
        String expiredToken = expiredJwtService.generateAccessToken("MockUserId", "MockUserNickname", List.of());

        assertThatThrownBy(() -> jwtService.parseClaims(expiredToken))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.EXPIRED_TOKEN));
    }

    @Test
    @DisplayName("위조된 토큰을 파싱하면 INVALID_TOKEN 예외가 발생한다")
    void parseClaims_tamperedToken_throwsInvalidToken() {
        JwtProperties tamperedProperties = new JwtProperties();
        tamperedProperties.setSecret("다-털렸어-다-털렸어-다-털렸어-다-털렸어-다-털렸어");
        tamperedProperties.setAccessTokenValidity(30L);
        tamperedProperties.setRefreshTokenValidity(7L);

        JwtService tamperedJwtService = new JwtService(tamperedProperties, redisTemplate);
        String tamperedToken = tamperedJwtService.generateAccessToken("user", "nickname", List.of());

        assertThatThrownBy(() -> jwtService.parseClaims(tamperedToken))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_TOKEN));
    }
}
