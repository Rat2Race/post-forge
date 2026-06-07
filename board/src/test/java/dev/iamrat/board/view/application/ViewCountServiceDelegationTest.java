package dev.iamrat.board.view.application;

import dev.iamrat.core.global.exception.CustomException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ViewCountServiceDelegationTest {

    private final ViewCountStrategy strategy = mock(ViewCountStrategy.class);
    private final ViewCountService service = new ViewCountService(strategy);

    @Test
    @DisplayName("단건 조회는 strategy로 위임한다")
    void getViewCount_delegatesStrategy() {
        given(strategy.getViewCount(1L)).willReturn(12L);

        assertThat(service.getViewCount(1L)).isEqualTo(12L);
    }

    @Test
    @DisplayName("목록 조회는 strategy로 위임한다")
    void getViewCounts_delegatesStrategy() {
        given(strategy.getViewCounts(List.of(1L, 2L))).willReturn(Map.of(1L, 12L, 2L, 7L));

        assertThat(service.getViewCounts(List.of(1L, 2L))).isEqualTo(Map.of(1L, 12L, 2L, 7L));
    }

    @Test
    @DisplayName("조회수 증가는 strategy로 위임한다")
    void incrementIfNew_delegatesStrategy() {
        service.incrementIfNew(1L, 10L);

        verify(strategy).incrementIfNew(1L, 10L);
    }

    @Test
    @DisplayName("null 입력은 strategy 호출 전에 거절한다")
    void nullInput_throwsInvalidInput() {
        assertThatThrownBy(() -> service.getViewCount(null))
            .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.incrementIfNew(1L, null))
            .isInstanceOf(CustomException.class);
    }
}
