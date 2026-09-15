package dev.iamrat.auth.account.infrastructure.persistence;

import dev.iamrat.auth.account.application.AccountStore;
import dev.iamrat.auth.account.domain.Account;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountStore {

    private final AccountRepository accountRepository;

    @Override
    public Account saveAndFlush(Account account) {
        return accountRepository.saveAndFlush(account);
    }

    @Override
    public void flush() {
        accountRepository.flush();
    }

    @Override
    public Optional<Account> findById(Long accountId) {
        return accountRepository.findById(accountId);
    }

    @Override
    public Optional<Account> findWithRolesById(Long accountId) {
        return accountRepository.findWithRolesById(accountId);
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    @Override
    public Optional<Account> findByProviderAndProviderId(String provider, String providerId) {
        return accountRepository.findByProviderAndProviderId(provider, providerId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return accountRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return accountRepository.existsByNickname(nickname);
    }

    @Override
    public boolean existsByEmail(String email) {
        return accountRepository.existsByEmail(email);
    }
}
