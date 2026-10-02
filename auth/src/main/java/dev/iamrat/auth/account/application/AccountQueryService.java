package dev.iamrat.auth.account.application;

import dev.iamrat.auth.account.domain.AccountRepository;
import dev.iamrat.auth.account.domain.Account;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryService {
    private final AccountRepository accountRepository;

    public Optional<Account> findById(Long accountId) {
        return accountRepository.findById(accountId);
    }

    public Optional<Account> findWithRolesById(Long accountId) {
        return accountRepository.findWithRolesById(accountId);
    }

    public Optional<Account> findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    public Optional<Account> findByProviderAndProviderId(String provider, String providerId) {
        return accountRepository.findByProviderAndProviderId(provider, providerId);
    }

    public boolean existsByUsername(String username) {
        return accountRepository.existsByUsername(username);
    }

    public boolean existsByNickname(String nickname) {
        return accountRepository.existsByNickname(nickname);
    }

    public boolean existsByEmail(String email) {
        return accountRepository.existsByEmail(email);
    }
}
