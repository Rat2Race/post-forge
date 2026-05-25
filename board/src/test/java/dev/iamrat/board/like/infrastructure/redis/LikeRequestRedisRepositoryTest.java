package dev.iamrat.board.like.infrastructure.redis;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LikeRequestRedisRepositoryTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LikeRequestRedisRepository repository;

    @Test
    @DisplayName("cooldown key가 없으면 TTL과 함께 key를 기록하고 요청을 허용한다")
    void markCooldownIfAbsent_whenNew_returnsTrue() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent("like:cooldown:post:like:1:1", "1", 1L, TimeUnit.SECONDS))
            .willReturn(true);

        boolean allowed = repository.markCooldownIfAbsent("post", 1L, 1L, "like");

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("cooldown key가 이미 있으면 요청을 막는다")
    void markCooldownIfAbsent_whenExisting_returnsFalse() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent("like:cooldown:post:like:1:1", "1", 1L, TimeUnit.SECONDS))
            .willReturn(false);

        boolean allowed = repository.markCooldownIfAbsent("post", 1L, 1L, "like");

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("계정별 rate count를 증가시킨다")
    void incrementRateCount_incrementsAccountRateKey() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("like:rate:1")).willReturn(2L);

        Long requestCount = repository.incrementRateCount(1L);

        assertThat(requestCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("첫 요청이면 rate key에 60초 window를 건다")
    void startRateWindow_expiresAccountRateKey() {
        repository.startRateWindow(1L);

        verify(redisTemplate).expire("like:rate:1", 60L, TimeUnit.SECONDS);
    }
}
