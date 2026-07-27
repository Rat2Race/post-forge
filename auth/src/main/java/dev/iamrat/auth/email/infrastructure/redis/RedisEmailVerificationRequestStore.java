package dev.iamrat.auth.email.infrastructure.redis;

import dev.iamrat.auth.email.application.EmailVerificationRequestDecision;
import dev.iamrat.auth.email.application.EmailVerificationRequestStore;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisEmailVerificationRequestStore implements EmailVerificationRequestStore {

    private static final DefaultRedisScript<String> EMAIL_REQUEST_GUARD_SCRIPT = emailRequestGuardScript();

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public EmailVerificationRequestDecision evaluateEmailRequest(
        String normalizedEmail,
        long cooldownSeconds,
        long windowSeconds,
        long limit,
        long lockSeconds
    ) {
        String decision = redisTemplate.execute(
            EMAIL_REQUEST_GUARD_SCRIPT,
            List.of(
                RedisEmailVerificationKeys.emailSendLockKey(normalizedEmail),
                RedisEmailVerificationKeys.emailSendRateKey(normalizedEmail),
                RedisEmailVerificationKeys.emailSendCooldownKey(normalizedEmail)
            ),
            Long.toString(windowSeconds),
            Long.toString(limit),
            Long.toString(lockSeconds),
            Long.toString(cooldownSeconds)
        );

        return EmailVerificationRequestDecision.fromCode(decision);
    }

    private static DefaultRedisScript<String> emailRequestGuardScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
            new ClassPathResource("redis/email-verification-request-guard.lua")
        ));
        script.setResultType(String.class);
        return script;
    }
}
