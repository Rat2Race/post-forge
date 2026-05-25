package dev.iamrat.auth.oauth.application;

import dev.iamrat.auth.account.application.AccountCommandService;
import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.account.domain.Account;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OAuth2AccountService {
    private final AccountQueryService accountQueryService;
    private final AccountCommandService accountCommandService;

    public Account getOrCreateAccount(String provider, OAuth2UserProfile userProfile) {
        String providerId = userProfile.getId();

        return accountQueryService.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() -> createOrFindAfterConcurrentCreate(provider, providerId, userProfile));
    }

    private Account createOrFindAfterConcurrentCreate(
        String provider,
        String providerId,
        OAuth2UserProfile userProfile
    ) {
        try {
            return accountCommandService.createOAuthAccount(
                provider,
                providerId,
                userProfile.getEmail(),
                generateUniqueNickname()
            );
        } catch (DataIntegrityViolationException exception) {
            return accountQueryService.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> exception);
        }
    }

    private String generateUniqueNickname() {
        for (int i = 0; i < 10; i++) {
            String nickname = "user_" + UUID.randomUUID().toString().substring(0, 8);
            if (!accountQueryService.existsByNickname(nickname)) {
                return nickname;
            }
        }
        // 10회 실패 시 타임스탬프 기반으로 유일성 보장
        return "user_" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100);
    }
}
