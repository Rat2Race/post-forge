package dev.iamrat.auth.oauth.service;

import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.repository.AccountRepository;
import dev.iamrat.auth.account.service.AccountService;
import dev.iamrat.auth.oauth.dto.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
@Service
public class OAuth2AccountService {
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public Account getOrCreateAccount(String provider, OAuth2UserInfo userInfo) {
        String providerId = userInfo.getId();

        return accountRepository.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() ->
                accountService.createAccount(
                    provider.toLowerCase() + "_" + providerId,
                    null,
                    userInfo.getEmail(),
                    generateUniqueNickname(),
                    provider,
                    providerId
                )
            );
    }

    private String generateUniqueNickname() {
        for (int i = 0; i < 10; i++) {
            String nickname = "user_" + UUID.randomUUID().toString().substring(0, 8);
            if (!accountRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }
        // 10회 실패 시 타임스탬프 기반으로 유일성 보장
        return "user_" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100);
    }
}
