package com.postforge.global.scheduler;

import com.postforge.domain.member.repository.EmailVerificationRepository;
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
        log.info("Starting cleanup of expired email verification tokens");
        try {
            emailVerificationRepository.deleteByExpiryDateBefore(LocalDateTime.now());
            log.info("Successfully cleaned up expired email verification tokens");
        } catch (Exception e) {
            log.error("Failed to cleanup expired email verification tokens", e);
        }
    }
}
