package dev.iamrat.auth.token.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.token.application.TokenLifetimeSettings;
import dev.iamrat.core.global.exception.CustomException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    TokenLifetimeSettings tokenLifetimeSettings = () -> 7L;

    RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = new RefreshTokenRepository(tokenLifetimeSettings, redisTemplate);
    }

    @Test
    @DisplayName("Redis에 토큰이 없으면 validate가 INVALID_TOKEN 예외를 던진다")
    void validate_notFound_throwsInvalidToken() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh_token:1")).willReturn(null);

        assertThatThrownBy(() -> refreshTokenRepository.validate(1L, "some-token"))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("저장된 토큰과 다르면 validate가 INVALID_TOKEN 예외를 던진다")
    void validate_mismatch_throwsInvalidToken() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh_token:1")).willReturn("correct-token");

        assertThatThrownBy(() -> refreshTokenRepository.validate(1L, "wrong-token"))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("리프레시 토큰은 accountId 키와 일 단위 TTL로 저장한다")
    void save_setsRefreshTokenWithTtl() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        refreshTokenRepository.save(1L, "refresh-token");

        verify(valueOperations).set("refresh_token:1", "refresh-token", 7L, TimeUnit.DAYS);
    }

    @Test
    @DisplayName("accountId로 리프레시 토큰을 삭제한다")
    void delete_removesRefreshToken() {
        refreshTokenRepository.delete(1L);

        verify(redisTemplate).delete("refresh_token:1");
    }
}
