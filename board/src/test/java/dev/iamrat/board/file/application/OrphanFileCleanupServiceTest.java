package dev.iamrat.board.file.application;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrphanFileCleanupServiceTest {

    private final FileStore fileStore = mock(FileStore.class);
    private final OrphanFileCleanupService cleanupService = new OrphanFileCleanupService(fileStore);

    @Test
    @DisplayName("24시간 이전 고아 파일을 정리한다")
    void cleanupOrphanFiles_deletesOrphanFilesOlderThanOneDay() {
        LocalDateTime before = LocalDateTime.now().minusHours(24);
        given(fileStore.deleteOrphanFilesBefore(any(LocalDateTime.class))).willReturn(3);

        cleanupService.cleanupOrphanFiles();

        var thresholdCaptor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(fileStore).deleteOrphanFilesBefore(thresholdCaptor.capture());
        LocalDateTime threshold = thresholdCaptor.getValue();
        assertThat(Duration.between(before, threshold)).isBetween(Duration.ZERO, Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("고아 파일 정리 실패는 호출자에게 전파하지 않는다")
    void cleanupOrphanFiles_whenStoreFails_doesNotPropagate() {
        given(fileStore.deleteOrphanFilesBefore(any(LocalDateTime.class))).willThrow(new RuntimeException("db down"));

        assertThatNoException().isThrownBy(cleanupService::cleanupOrphanFiles);
    }
}
