package dev.iamrat.board.like.application;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class LikeRequestGuardTest {

    @Mock
    private LikeRequestWindow likeRequestWindow;

    private LikeRequestGuard likeRequestGuard;

    @BeforeEach
    void setUp() {
        likeRequestGuard = new LikeRequestGuard(likeRequestWindow);
    }

    @Test
    @DisplayName("첫 like 요청은 cooldown과 rate limit을 통과한다")
    void guardPostLike_whenFirstRequest_allows() {
        given(likeRequestWindow.markCooldownIfAbsent("post", 1L, 1L, "like")).willReturn(true);
        given(likeRequestWindow.incrementRateCount(1L)).willReturn(1L);

        likeRequestGuard.guardPostLike(1L, 1L);

        verify(likeRequestWindow).startRateWindow(1L);
    }

    @Test
    @DisplayName("같은 액션을 cooldown 안에 반복하면 TOO_MANY_REQUESTS 예외를 던진다")
    void guardPostLike_whenCooldownHit_throwsTooManyRequests() {
        given(likeRequestWindow.markCooldownIfAbsent("post", 1L, 1L, "like")).willReturn(false);

        assertThatThrownBy(() -> likeRequestGuard.guardPostLike(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("분당 요청 수가 제한을 넘으면 TOO_MANY_REQUESTS 예외를 던진다")
    void guardPostUnlike_whenRateExceeded_throwsTooManyRequests() {
        given(likeRequestWindow.markCooldownIfAbsent("post", 1L, 1L, "unlike")).willReturn(true);
        given(likeRequestWindow.incrementRateCount(1L)).willReturn(31L);

        assertThatThrownBy(() -> likeRequestGuard.guardPostUnlike(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("Redis 장애가 나면 fail-open으로 요청을 통과시킨다")
    void guardCommentLike_whenRedisFails_allowsRequest() {
        given(likeRequestWindow.markCooldownIfAbsent("comment", 7L, 1L, "like"))
            .willThrow(new RuntimeException("redis down"));

        likeRequestGuard.guardCommentLike(7L, 1L);

        verify(likeRequestWindow, never()).incrementRateCount(1L);
    }
}
