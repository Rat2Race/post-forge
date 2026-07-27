package dev.iamrat.board.like.infrastructure.redis;

import dev.iamrat.support.redis.RedisGuardOperations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LikeRequestRedisRepositoryTest {

    @Mock
    private RedisGuardOperations redisGuardOperations;

    @InjectMocks
    private LikeRequestRedisRepository repository;

    @Test
    @DisplayName("쿨다운 키가 없으면 TTL과 함께 키를 기록하고 요청을 허용한다")
    void markCooldownIfAbsent_whenNew_returnsTrue() {
        given(redisGuardOperations.markIfAbsent("like:cooldown:post:like:1:1", 1L)).willReturn(true);

        boolean allowed = repository.markCooldownIfAbsent("post", 1L, 1L, "like");

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("쿨다운 키가 이미 있으면 요청을 막는다")
    void markCooldownIfAbsent_whenExisting_returnsFalse() {
        given(redisGuardOperations.markIfAbsent("like:cooldown:post:like:1:1", 1L)).willReturn(false);

        boolean allowed = repository.markCooldownIfAbsent("post", 1L, 1L, "like");

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("계정별 rate count를 window TTL과 함께 증가시킨다")
    void incrementRateCount_incrementsAccountRateKeyWithExpiry() {
        given(redisGuardOperations.incrementWithExpiry("like:rate:1", 60L)).willReturn(2L);

        Long requestCount = repository.incrementRateCount(1L, 60L);

        assertThat(requestCount).isEqualTo(2L);
        verify(redisGuardOperations).incrementWithExpiry("like:rate:1", 60L);
    }
}
