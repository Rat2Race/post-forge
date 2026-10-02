package dev.iamrat.support.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 실제 Redis에서 횟수 제한 카운터의 원자성을 확인한다. */
@Tag("integration")
@Testcontainers
class RedisGuardOperationsContainerTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
    }

    @AfterAll
    static void disconnect() {
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("INCR 뒤 만료 설정이 실패해도 만료 없이 영원히 남는 카운터 키가 생기지 않는다")
    void expireFailureDoesNotLeaveCounterWithoutTtl() {
        RedisTemplate<String, String> failingExpire = spy(redisTemplate);
        doThrow(new RedisSystemException("만료 설정 중 연결이 끊겼다", null))
            .when(failingExpire).expire(anyString(), anyLong(), any(TimeUnit.class));
        String key = "guard:test:" + UUID.randomUUID();

        try {
            new RedisGuardOperations(failingExpire).incrementWithExpiry(key, 60);
        } catch (RedisSystemException ignored) {
            // 수정 전 구현은 INCR를 반영한 뒤 여기서 실패한다.
        }

        // -2(키 없음)나 양수(만료 있음)는 괜찮다. -1은 만료 없는 카운터라 그 키의 요청이 영원히 막힌다.
        assertThat(redisTemplate.getExpire(key)).isNotEqualTo(-1L);
    }

    @Test
    @DisplayName("100번 동시에 늘려도 값은 100이고 만료가 걸려 있다")
    void concurrentIncrementsKeepCountAndTtl() throws Exception {
        RedisGuardOperations operations = new RedisGuardOperations(redisTemplate);
        String key = "guard:test:" + UUID.randomUUID();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Future<Long>> results = IntStream.range(0, 100)
                .mapToObj(i -> pool.submit(() -> operations.incrementWithExpiry(key, 60)))
                .toList();
            for (Future<Long> result : results) {
                result.get();
            }
        } finally {
            pool.shutdown();
        }

        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("100");
        assertThat(redisTemplate.getExpire(key)).isPositive();
    }

    @Test
    @DisplayName("예전 결함으로 만료 없이 남은 카운터도 다음 증가 때 만료가 걸린다")
    void legacyCounterWithoutTtlGetsExpiryOnNextIncrement() {
        String key = "guard:test:" + UUID.randomUUID();
        redisTemplate.opsForValue().set(key, "5");

        new RedisGuardOperations(redisTemplate).incrementWithExpiry(key, 60);

        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("6");
        assertThat(redisTemplate.getExpire(key)).isPositive();
    }
}
