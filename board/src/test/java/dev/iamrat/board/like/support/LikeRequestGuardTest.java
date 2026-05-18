package dev.iamrat.board.like.support;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class LikeRequestGuardTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LikeRequestGuard likeRequestGuard;

    @BeforeEach
    void setUp() {
        likeRequestGuard = new LikeRequestGuard(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("첫 like 요청은 cooldown과 rate limit을 통과한다")
    void guardPostLike_whenFirstRequest_allows() {
        given(valueOperations.setIfAbsent("like:cooldown:post:like:1:user1", "1", 1L, TimeUnit.SECONDS)).willReturn(true);
        given(valueOperations.increment("like:rate:user1")).willReturn(1L);

        likeRequestGuard.guardPostLike(1L, "user1");

        verify(redisTemplate).expire("like:rate:user1", 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("같은 액션을 cooldown 안에 반복하면 TOO_MANY_REQUESTS 예외를 던진다")
    void guardPostLike_whenCooldownHit_throwsTooManyRequests() {
        given(valueOperations.setIfAbsent("like:cooldown:post:like:1:user1", "1", 1L, TimeUnit.SECONDS)).willReturn(false);

        assertThatThrownBy(() -> likeRequestGuard.guardPostLike(1L, "user1"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("분당 요청 수가 제한을 넘으면 TOO_MANY_REQUESTS 예외를 던진다")
    void guardPostUnlike_whenRateExceeded_throwsTooManyRequests() {
        given(valueOperations.setIfAbsent("like:cooldown:post:unlike:1:user1", "1", 1L, TimeUnit.SECONDS)).willReturn(true);
        given(valueOperations.increment("like:rate:user1")).willReturn(31L);

        assertThatThrownBy(() -> likeRequestGuard.guardPostUnlike(1L, "user1"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("Redis 장애가 나면 fail-open으로 요청을 통과시킨다")
    void guardCommentLike_whenRedisFails_allowsRequest() {
        given(valueOperations.setIfAbsent("like:cooldown:comment:like:7:user1", "1", 1L, TimeUnit.SECONDS))
                .willThrow(new RuntimeException("redis down"));

        likeRequestGuard.guardCommentLike(7L, "user1");

        verify(valueOperations, never()).increment("like:rate:user1");
    }
}
