package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
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
class ViewCountServiceTest {

    @Mock
    private PostViewCountService postViewCountService;

    @InjectMocks
    private ViewCountService viewCountService;

    @Test
    @DisplayName("조회수는 DB 값을 직접 반환한다")
    void getViewCount_loadsFromDb() {
        given(postViewCountService.getViewCount(1L)).willReturn(12L);

        long viewCount = viewCountService.getViewCount(1L);

        assertThat(viewCount).isEqualTo(12L);
        verify(postViewCountService).getViewCount(1L);
    }

    @Test
    @DisplayName("조회수 증가는 DB에 직접 반영한다")
    void incrementIfNew_incrementsDbCount() {
        viewCountService.incrementIfNew(1L, 10L);

        verify(postViewCountService).incrementViewCount(1L);
    }
}
