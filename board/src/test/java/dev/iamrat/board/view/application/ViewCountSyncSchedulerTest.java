package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewCountSyncSchedulerTest {

    @Mock
    private ViewCountStore viewCountStore;

    @Mock
    private PostViewCountService postViewCountService;

    @InjectMocks
    private ViewCountSyncScheduler scheduler;

    @Test
    @DisplayName("동기화 중 DB 업데이트가 실패하면 processing set에서 제거하지 않는다")
    void syncViewCountsToDb_keepsDirtyIdsWhenUpdateFails() {
        given(viewCountStore.claimDirtyIdsForProcessing())
            .willReturn(Optional.of("post:views:dirty:processing"));
        given(viewCountStore.findDirtyIds("post:views:dirty:processing")).willReturn(Set.of("1"));
        given(viewCountStore.findViewCount(1L)).willReturn(Optional.of(10L));
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
            .when(postViewCountService).updateViewCount(1L, 10L);

        scheduler.syncViewCountsToDb();

        verify(viewCountStore).removeProcessedDirtyIds("post:views:dirty:processing", Set.of());
    }

    @Test
    @DisplayName("성공적으로 동기화한 ID는 processing set에서 제거한다")
    void syncViewCountsToDb_removesProcessedIdsAfterSuccess() {
        given(viewCountStore.claimDirtyIdsForProcessing())
            .willReturn(Optional.of("post:views:dirty:processing"));
        given(viewCountStore.findDirtyIds("post:views:dirty:processing")).willReturn(Set.of("1"));
        given(viewCountStore.findViewCount(1L)).willReturn(Optional.of(11L));

        scheduler.syncViewCountsToDb();

        verify(postViewCountService).updateViewCount(1L, 11L);
        verify(viewCountStore).removeProcessedDirtyIds("post:views:dirty:processing", Set.of("1"));
    }
}
