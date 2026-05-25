package dev.iamrat.auth.token.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.token.application.TokenClaims;
import dev.iamrat.core.global.exception.CustomException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class JwtProviderTest {

    JwtProperties jwtProperties = new JwtProperties();

    JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProperties.setSecret("test-secret-key-test-secret-key-test-secret-key");
        jwtProperties.setAccessTokenValidity(30L);
        jwtProperties.setRefreshTokenValidity(7L);

        jwtProvider = new JwtProvider(jwtProperties);
    }

    @Test
    @DisplayName("올바른 파라미터로 생성한 토큰을 파싱하면 subject와 roles가 일치한다")
    void generateToken_validParams_claimsMatch() {
        Long accountId = 1L;
        List<String> authorities = List.of("ROLE_USER");

        String accessToken = jwtProvider.generateAccessToken(accountId, authorities);
        TokenClaims claims = jwtProvider.parse(accessToken);

        assertThat(claims.subject()).isEqualTo("1");
        assertThat(claims.roles()).isEqualTo(List.of("ROLE_USER"));
    }

    @Test
    @DisplayName("잘못된 파라미터로 토큰 생성하면 INVALID_TOKEN 예외가 발생한다")
    void generateToken_blankSubject_throwsInvalidToken() {
        assertThatThrownBy(() -> jwtProvider.generateAccessToken(null, List.of()))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("만료된 토큰을 파싱하면 EXPIRED_TOKEN 예외가 발생한다")
    void parseClaims_expiredToken_throwsExpiredToken() {
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret("test-secret-key-test-secret-key-test-secret-key");
        expiredProperties.setAccessTokenValidity(0L);
        expiredProperties.setRefreshTokenValidity(0L);

        JwtProvider expiredJwtProvider = new JwtProvider(expiredProperties);
        String expiredToken = expiredJwtProvider.generateAccessToken(1L, List.of());

        assertThatThrownBy(() -> jwtProvider.parse(expiredToken))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.EXPIRED_TOKEN));
    }

    @Test
    @DisplayName("위조된 토큰을 파싱하면 INVALID_TOKEN 예외가 발생한다")
    void parseClaims_tamperedToken_throwsInvalidToken() {
        JwtProperties tamperedProperties = new JwtProperties();
        tamperedProperties.setSecret("다-털렸어-다-털렸어-다-털렸어-다-털렸어-다-털렸어");
        tamperedProperties.setAccessTokenValidity(30L);
        tamperedProperties.setRefreshTokenValidity(7L);

        JwtProvider tamperedJwtProvider = new JwtProvider(tamperedProperties);
        String tamperedToken = tamperedJwtProvider.generateAccessToken(1L, List.of());

        assertThatThrownBy(() -> jwtProvider.parse(tamperedToken))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }
}
