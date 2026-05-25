package dev.iamrat.board.view.infrastructure.redis;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewCountRedisRepositoryTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private ViewCountRedisRepository repository;

    @Test
    @DisplayName("조회수 캐시를 읽고 TTL을 갱신한다")
    void findViewCountAndRefreshTtl_refreshesTtlOnHit() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("post:views:1")).willReturn("12");

        Optional<Long> viewCount = repository.findViewCountAndRefreshTtl(1L);

        assertThat(viewCount).contains(12L);
        verify(redisTemplate).expire("post:views:1", 86_400L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("사용자별 조회 guard key가 없을 때 첫 조회로 판단한다")
    void markViewedIfAbsent_returnsTrueWhenGuardIsNew() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent("post:viewed:1:10", "Viewed", 24L, TimeUnit.HOURS))
            .willReturn(true);

        boolean firstView = repository.markViewedIfAbsent(1L, 10L);

        assertThat(firstView).isTrue();
    }

    @Test
    @DisplayName("처리 완료한 dirty ID를 제거하고 남은 ID가 없으면 processing key를 삭제한다")
    void removeProcessedDirtyIds_deletesEmptyProcessingSet() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.size("post:views:dirty:processing")).willReturn(0L);

        repository.removeProcessedDirtyIds("post:views:dirty:processing", Set.of("1"));

        verify(setOperations).remove("post:views:dirty:processing", "1");
        verify(redisTemplate).delete("post:views:dirty:processing");
    }

    @Test
    @DisplayName("dirty set을 processing set으로 claim한다")
    void claimDirtyIdsForProcessing_renamesDirtySet() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.members("post:views:dirty:processing")).willReturn(Set.of());

        Optional<String> processingKey = repository.claimDirtyIdsForProcessing();

        assertThat(processingKey).contains("post:views:dirty:processing");
        verify(redisTemplate).rename("post:views:dirty", "post:views:dirty:processing");
    }
}
