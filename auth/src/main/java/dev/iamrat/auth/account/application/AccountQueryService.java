package dev.iamrat.auth.account.application;

import dev.iamrat.auth.account.domain.Account;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryService {
    private final AccountStore accountStore;

    public Optional<Account> findById(Long accountId) {
        return accountStore.findById(accountId);
    }

    public Optional<Account> findWithRolesById(Long accountId) {
        return accountStore.findWithRolesById(accountId);
    }

    public Optional<Account> findByUsername(String username) {
        return accountStore.findByUsername(username);
    }

    public Optional<Account> findByProviderAndProviderId(String provider, String providerId) {
        return accountStore.findByProviderAndProviderId(provider, providerId);
    }

    public boolean existsByUsername(String username) {
        return accountStore.existsByUsername(username);
    }

    public boolean existsByNickname(String nickname) {
        return accountStore.existsByNickname(nickname);
    }

    public boolean existsByEmail(String email) {
        return accountStore.existsByEmail(email);
    }
}
