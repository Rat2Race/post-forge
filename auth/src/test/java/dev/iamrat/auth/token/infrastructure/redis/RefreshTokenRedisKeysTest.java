package dev.iamrat.auth.token.infrastructure.redis;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenRedisKeysTest {

    @Test
    @DisplayName("refresh token key는 accountId 단위로 생성한다")
    void refreshTokenKey_includesAccountId() {
        assertThat(RefreshTokenRedisKeys.refreshTokenKey(1L))
            .isEqualTo("refresh_token:1");
    }

    @Test
    @DisplayName("accountId가 null이면 INVALID_TOKEN 예외를 던진다")
    void refreshTokenKey_nullAccountId_throwsInvalidToken() {
        assertThatThrownBy(() -> RefreshTokenRedisKeys.refreshTokenKey(null))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }
}
