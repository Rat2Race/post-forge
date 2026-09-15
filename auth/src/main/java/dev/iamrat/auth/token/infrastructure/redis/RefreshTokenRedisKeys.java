package dev.iamrat.auth.token.infrastructure.redis;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;

final class RefreshTokenRedisKeys {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    private RefreshTokenRedisKeys() {
    }

    static String refreshTokenKey(Long accountId) {
        if (accountId == null) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
        return REFRESH_TOKEN_PREFIX + accountId;
    }
}
