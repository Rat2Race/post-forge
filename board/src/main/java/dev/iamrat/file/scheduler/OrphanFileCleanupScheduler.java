package dev.iamrat.file.scheduler;

import dev.iamrat.file.repository.FileRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFileCleanupScheduler {

    private final FileRepository fileRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOrphanFiles() {
        log.info("고아 파일 정리 시작");
        try {
            LocalDateTime threshold = LocalDateTime.now().minusHours(24);
            int count = fileRepository.deleteOrphanFiles(threshold);
            if (count == 0) {
                log.info("정리할 고아 파일 없음");
                return;
            }
            log.info("고아 파일 {}건 정리 완료", count);
        } catch (Exception e) {
            log.error("고아 파일 정리 실패", e);
        }
    }
}
