package dev.iamrat.auth.email.infrastructure.redis;

import dev.iamrat.auth.email.application.EmailVerificationStore;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisEmailVerificationStore implements EmailVerificationStore {
    private static final long TOKEN_TTL_MINUTES = 30;
    private static final long VERIFIED_TTL_HOURS = 1;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void saveToken(String token, String email) {
        redisTemplate.opsForValue().set(
            RedisEmailVerificationKeys.tokenToEmailKey(token),
            email,
            TOKEN_TTL_MINUTES,
            TimeUnit.MINUTES
        );
    }

    @Override
    public String getEmailAndDeleteToken(String token) {
        return redisTemplate.opsForValue().getAndDelete(RedisEmailVerificationKeys.tokenToEmailKey(token));
    }

    @Override
    public void markVerified(String email) {
        redisTemplate.opsForValue().set(
            RedisEmailVerificationKeys.emailVerifiedKey(email),
            Boolean.TRUE.toString(),
            VERIFIED_TTL_HOURS,
            TimeUnit.HOURS
        );
    }

    @Override
    public boolean isVerified(String email) {
        return Boolean.TRUE.toString().equals(
            redisTemplate.opsForValue().get(RedisEmailVerificationKeys.emailVerifiedKey(email))
        );
    }

    @Override
    public void removeVerified(String email) {
        redisTemplate.delete(RedisEmailVerificationKeys.emailVerifiedKey(email));
    }
}
