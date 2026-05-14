package dev.iamrat.oauth.service;

import dev.iamrat.auth.exception.AuthErrorCode;
import dev.iamrat.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OAuth2CodeService {
    private static final String CODE_PREFIX = "oauth2_code:";
    private static final long CODE_TTL_SECONDS = 60;

    private final RedisTemplate<String, String> redisTemplate;

    public String createCode(String userId) {
        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                CODE_PREFIX + code,
                userId,
                CODE_TTL_SECONDS,
                TimeUnit.SECONDS
        );
        return code;
    }

    public String exchangeCode(String code) {
        String key = CODE_PREFIX + code;
        String userId = redisTemplate.opsForValue().getAndDelete(key);
        if (userId == null) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
        return userId;
    }
}
