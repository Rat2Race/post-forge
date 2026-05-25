package dev.iamrat.auth.token.infrastructure.redis;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.token.application.RefreshTokenStore;
import dev.iamrat.auth.token.application.TokenLifetimeSettings;
import dev.iamrat.core.global.exception.CustomException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository implements RefreshTokenStore {
    private final TokenLifetimeSettings tokenLifetimeSettings;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void validate(Long accountId, String requestToken) {
        String storedToken = redisTemplate.opsForValue().get(RefreshTokenRedisKeys.refreshTokenKey(accountId));
        if (storedToken == null) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        byte[] stored = storedToken.getBytes(StandardCharsets.UTF_8);
        byte[] request = requestToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(stored, request)) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public void save(Long accountId, String refreshToken) {
        redisTemplate.opsForValue().set(
            RefreshTokenRedisKeys.refreshTokenKey(accountId),
            refreshToken,
            tokenLifetimeSettings.refreshTokenValidityDays(),
            TimeUnit.DAYS
        );
    }

    @Override
    public void delete(Long accountId) {
        redisTemplate.delete(RefreshTokenRedisKeys.refreshTokenKey(accountId));
    }
}
