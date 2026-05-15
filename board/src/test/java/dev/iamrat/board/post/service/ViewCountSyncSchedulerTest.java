package dev.iamrat.board.post.service;

import dev.iamrat.board.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewCountSyncSchedulerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private PostRepository postRepository;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ViewCountSyncScheduler scheduler;

    @Test
    @DisplayName("동기화 중 DB 업데이트가 실패하면 processing set에서 제거하지 않는다")
    void syncViewCountsToDb_keepsDirtyIdsWhenUpdateFails() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(setOperations.members("post:views:dirty:processing")).willReturn(Set.of("1"));
        given(valueOperations.get("post:views:1")).willReturn("10");
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
            .when(postRepository).updateViews(1L, 10L);

        scheduler.syncViewCountsToDb();

        verify(setOperations, never()).remove(anyString(), any());
        verify(redisTemplate, never()).delete("post:views:dirty:processing");
    }

    @Test
    @DisplayName("성공적으로 동기화한 ID는 processing set에서 제거한다")
    void syncViewCountsToDb_removesProcessedIdsAfterSuccess() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(setOperations.members("post:views:dirty:processing")).willReturn(Set.of("1"));
        given(valueOperations.get("post:views:1")).willReturn("11");
        given(setOperations.size("post:views:dirty:processing")).willReturn(0L);

        scheduler.syncViewCountsToDb();

        verify(postRepository).updateViews(1L, 11L);
        verify(setOperations).remove("post:views:dirty:processing", "1");
        verify(redisTemplate).delete("post:views:dirty:processing");
    }
}
