package dev.iamrat.auth.account.application;

import dev.iamrat.auth.account.domain.Account;
import java.util.Optional;

public interface AccountStore {

    Account saveAndFlush(Account account);

    void flush();

    Optional<Account> findById(Long accountId);

    Optional<Account> findWithRolesById(Long accountId);

    Optional<Account> findByUsername(String username);

    Optional<Account> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);
}
