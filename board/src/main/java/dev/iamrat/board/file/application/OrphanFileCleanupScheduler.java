package dev.iamrat.board.file.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrphanFileCleanupScheduler {

    private final OrphanFileCleanupService orphanFileCleanupService;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOrphanFiles() {
        orphanFileCleanupService.cleanupOrphanFiles();
    }
}
