package dev.iamrat.support.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
class RedisGuardOperationsTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("카운터 첫 증가면 TTL을 설정한다")
    void incrementWithExpiry_whenFirstCount_setsExpiry() {
        RedisGuardOperations operations = new RedisGuardOperations(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("guard:rate:test")).willReturn(1L);

        Long count = operations.incrementWithExpiry("guard:rate:test", 60L);

        assertThat(count).isEqualTo(1L);
        verify(redisTemplate).expire("guard:rate:test", 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("카운터 첫 증가가 아니면 TTL을 다시 설정하지 않는다")
    void incrementWithExpiry_whenExistingCount_doesNotResetExpiry() {
        RedisGuardOperations operations = new RedisGuardOperations(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("guard:rate:test")).willReturn(2L);

        Long count = operations.incrementWithExpiry("guard:rate:test", 60L);

        assertThat(count).isEqualTo(2L);
        verify(redisTemplate, never()).expire("guard:rate:test", 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("키가 없으면 마커와 TTL을 저장하고 true를 반환한다")
    void markIfAbsent_whenNew_returnsTrue() {
        RedisGuardOperations operations = new RedisGuardOperations(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent("guard:cooldown:test", "1", 30L, TimeUnit.SECONDS))
            .willReturn(true);

        assertThat(operations.markIfAbsent("guard:cooldown:test", 30L)).isTrue();
    }

    @Test
    @DisplayName("키가 이미 있으면 false를 반환한다")
    void markIfAbsent_whenExisting_returnsFalse() {
        RedisGuardOperations operations = new RedisGuardOperations(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent("guard:cooldown:test", "1", 30L, TimeUnit.SECONDS))
            .willReturn(false);

        assertThat(operations.markIfAbsent("guard:cooldown:test", 30L)).isFalse();
    }

    @Test
    @DisplayName("Redis가 null을 반환하면 false로 해석한다")
    void markIfAbsent_whenRedisReturnsNull_returnsFalse() {
        RedisGuardOperations operations = new RedisGuardOperations(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent("guard:cooldown:test", "1", 30L, TimeUnit.SECONDS))
            .willReturn(null);

        assertThat(operations.markIfAbsent("guard:cooldown:test", 30L)).isFalse();
    }

    @Test
    @DisplayName("TTL 마커를 저장한다")
    void mark_setsMarkerWithTtl() {
        RedisGuardOperations operations = new RedisGuardOperations(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        operations.mark("guard:lock:test", 300L);

        verify(valueOperations).set("guard:lock:test", "1", 300L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("키가 있으면 true를 반환한다")
    void hasKey_whenRedisReturnsTrue_returnsTrue() {
        RedisGuardOperations operations = new RedisGuardOperations(redisTemplate);
        given(redisTemplate.hasKey("guard:lock:test")).willReturn(true);

        assertThat(operations.hasKey("guard:lock:test")).isTrue();
    }

    @Test
    @DisplayName("Redis가 키 조회에 null을 반환하면 false로 해석한다")
    void hasKey_whenRedisReturnsNull_returnsFalse() {
        RedisGuardOperations operations = new RedisGuardOperations(redisTemplate);
        given(redisTemplate.hasKey("guard:lock:test")).willReturn(null);

        assertThat(operations.hasKey("guard:lock:test")).isFalse();
    }

    @Test
    @DisplayName("여러 키 삭제를 RedisTemplate에 위임한다")
    void delete_delegatesKeys() {
        RedisGuardOperations operations = new RedisGuardOperations(redisTemplate);
        List<String> keys = List.of("guard:first", "guard:second");

        operations.delete(keys);

        verify(redisTemplate).delete(keys);
    }
}
