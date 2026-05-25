package dev.iamrat.board.file.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrphanFileCleanupSchedulerTest {

    private final OrphanFileCleanupService cleanupService = mock(OrphanFileCleanupService.class);
    private final OrphanFileCleanupScheduler scheduler = new OrphanFileCleanupScheduler(cleanupService);

    @Test
    @DisplayName("스케줄 트리거는 고아 파일 정리 유스케이스에 위임한다")
    void cleanupOrphanFiles_delegatesToCleanupService() {
        scheduler.cleanupOrphanFiles();

        verify(cleanupService).cleanupOrphanFiles();
    }
}
