package dev.iamrat.auth.login.infrastructure.redis;

import dev.iamrat.auth.login.application.LoginAttemptDecision;
import dev.iamrat.auth.login.application.LoginAttemptEvaluation;
import dev.iamrat.auth.login.application.LoginAttemptLimiter;
import dev.iamrat.auth.login.application.LoginFailureDecision;
import dev.iamrat.auth.login.application.LoginFailureRecord;
import dev.iamrat.support.redis.RedisGuardOperations;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisLoginAttemptLimiter implements LoginAttemptLimiter {
    private final RedisGuardOperations redisGuardOperations;

    @Override
    public LoginAttemptDecision evaluate(LoginAttemptEvaluation evaluation) {
        if (redisGuardOperations.hasKey(RedisLoginAttemptKeys.lockKey(evaluation.normalizedUsername()))) {
            return LoginAttemptDecision.LOCKED;
        }

        Long userRate = redisGuardOperations.incrementWithExpiry(
            RedisLoginAttemptKeys.usernameRateKey(evaluation.normalizedUsername()),
            evaluation.windowSeconds()
        );

        if (exceeds(userRate, evaluation.userLimit())) {
            return LoginAttemptDecision.USER_RATE_LIMITED;
        }

        if (!isBlank(evaluation.clientIp())) {
            Long ipRate = redisGuardOperations.incrementWithExpiry(
                RedisLoginAttemptKeys.ipRateKey(evaluation.clientIp()),
                evaluation.windowSeconds()
            );
            if (exceeds(ipRate, evaluation.ipLimit())) {
                return LoginAttemptDecision.IP_RATE_LIMITED;
            }
        }

        return LoginAttemptDecision.ALLOWED;
    }

    @Override
    public LoginFailureDecision recordFailure(LoginFailureRecord record) {
        Long failures = redisGuardOperations.incrementWithExpiry(
            RedisLoginAttemptKeys.failureKey(record.normalizedUsername()),
            record.windowSeconds()
        );

        if (failures != null && failures >= record.failureLimit()) {
            redisGuardOperations.mark(
                RedisLoginAttemptKeys.lockKey(record.normalizedUsername()),
                record.lockSeconds()
            );
            return LoginFailureDecision.LOCKED;
        }

        return LoginFailureDecision.RECORDED;
    }

    @Override
    public void clearFailure(String normalizedUsername) {
        redisGuardOperations.delete(List.of(
            RedisLoginAttemptKeys.failureKey(normalizedUsername),
            RedisLoginAttemptKeys.lockKey(normalizedUsername)
        ));
    }

    private boolean exceeds(Long count, long limit) {
        return count != null && count > limit;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
