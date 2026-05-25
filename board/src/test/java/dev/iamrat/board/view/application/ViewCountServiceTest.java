package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewCountServiceTest {

    @Mock
    private ViewCountStore viewCountStore;

    @Mock
    private PostViewCountService postViewCountService;

    @InjectMocks
    private ViewCountService viewCountService;

    @Test
    @DisplayName("캐시에 조회수가 없으면 DB 값을 캐시에 적재하고 반환한다")
    void getViewCount_cacheMiss_loadsFromDb() {
        given(viewCountStore.findViewCountAndRefreshTtl(1L)).willReturn(Optional.empty());
        given(postViewCountService.getViewCount(1L)).willReturn(12L);

        long viewCount = viewCountService.getViewCount(1L);

        assertThat(viewCount).isEqualTo(12L);
        verify(viewCountStore).cacheViewCountIfAbsent(1L, 12L);
    }

    @Test
    @DisplayName("24시간 내 첫 조회만 조회수를 증가시키고 dirty set에 등록한다")
    void incrementIfNew_firstView_incrementsAndMarksDirty() {
        given(viewCountStore.markViewedIfAbsent(1L, 10L)).willReturn(true);
        given(viewCountStore.findViewCount(1L)).willReturn(Optional.of(12L));

        viewCountService.incrementIfNew(1L, 10L);

        verify(viewCountStore).incrementViewCount(1L);
        verify(viewCountStore).markDirty(1L);
        verify(postViewCountService, never()).getViewCount(anyLong());
    }
}
