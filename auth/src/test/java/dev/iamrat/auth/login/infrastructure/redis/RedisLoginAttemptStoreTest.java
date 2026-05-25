package dev.iamrat.auth.login.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.TimeUnit;
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
class RedisLoginAttemptStoreTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("사용자 rate counter 첫 증가면 TTL을 설정한다")
    void incrementUserRate_firstCounter_setsExpiry() {
        RedisLoginAttemptStore store = new RedisLoginAttemptStore(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("auth:login:rate:user:testuser1")).willReturn(1L);

        Long result = store.incrementUserRate("testuser1", 60L);

        assertThat(result).isEqualTo(1L);
        verify(redisTemplate).expire("auth:login:rate:user:testuser1", 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("IP rate counter 첫 증가면 TTL을 설정한다")
    void incrementIpRate_firstCounter_setsExpiry() {
        RedisLoginAttemptStore store = new RedisLoginAttemptStore(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("auth:login:rate:ip:127.0.0.1")).willReturn(1L);

        Long result = store.incrementIpRate("127.0.0.1", 60L);

        assertThat(result).isEqualTo(1L);
        verify(redisTemplate).expire("auth:login:rate:ip:127.0.0.1", 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("로그인 실패 counter 첫 증가면 TTL을 설정한다")
    void incrementFailure_firstCounter_setsExpiry() {
        RedisLoginAttemptStore store = new RedisLoginAttemptStore(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("auth:login:fail:testuser1")).willReturn(1L);

        Long result = store.incrementFailure("testuser1", 300L);

        assertThat(result).isEqualTo(1L);
        verify(redisTemplate).expire("auth:login:fail:testuser1", 300L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("사용자 잠금 여부를 lock key로 조회한다")
    void hasLock_delegatesLockKeyLookup() {
        RedisLoginAttemptStore store = new RedisLoginAttemptStore(redisTemplate);
        given(redisTemplate.hasKey("auth:login:lock:testuser1")).willReturn(true);

        assertThat(store.hasLock("testuser1")).isTrue();
    }

    @Test
    @DisplayName("사용자 잠금은 lock key와 초 단위 TTL로 저장한다")
    void lock_setsLockKeyWithTtl() {
        RedisLoginAttemptStore store = new RedisLoginAttemptStore(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        store.lock("testuser1", 300L);

        verify(valueOperations).set("auth:login:lock:testuser1", "1", 300L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("로그인 성공 시 실패 카운터와 잠금 키를 제거한다")
    void clearFailureAndLock_deletesFailureAndLockKeys() {
        RedisLoginAttemptStore store = new RedisLoginAttemptStore(redisTemplate);

        store.clearFailureAndLock("testuser1");

        verify(redisTemplate).delete(List.of("auth:login:fail:testuser1", "auth:login:lock:testuser1"));
    }
}
