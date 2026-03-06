package dev.iamrat.email.scheduler;

import dev.iamrat.email.repository.EmailVerificationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationCleanupScheduler {
    private final EmailVerificationRepository emailVerificationRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("만료된 이메일 인증 토큰 정리 시작");
        try {
            int count = emailVerificationRepository.deleteByExpiryDateBefore(LocalDateTime.now());
            if (count == 0) {
                log.info("정리할 만료 토큰 없음");
                return;
            }
            log.info("만료된 이메일 인증 토큰 {}건 정리 완료", count);
        } catch (Exception e) {
            log.error("만료된 이메일 인증 토큰 정리 실패", e);
        }
    }
}
