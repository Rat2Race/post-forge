package dev.iamrat.auth.account.application;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryService {
    private final AccountStore accountStore;

    public Account findWithRolesById(Long accountId) {
        return accountStore.findWithRolesById(accountId)
            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    public Optional<Account> findByUsername(String username) {
        return accountStore.findByUsername(username);
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
