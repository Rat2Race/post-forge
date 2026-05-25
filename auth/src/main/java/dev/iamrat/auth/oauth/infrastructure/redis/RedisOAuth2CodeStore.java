package dev.iamrat.auth.oauth.infrastructure.redis;

import dev.iamrat.auth.oauth.application.OAuth2CodeStore;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisOAuth2CodeStore implements OAuth2CodeStore {
    private static final long CODE_TTL_SECONDS = 60;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(String code, Long accountId) {
        redisTemplate.opsForValue().set(
            RedisOAuth2CodeKeys.exchangeCodeKey(code),
            String.valueOf(accountId),
            CODE_TTL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    @Override
    public String getAndDelete(String code) {
        return redisTemplate.opsForValue().getAndDelete(RedisOAuth2CodeKeys.exchangeCodeKey(code));
    }
}
