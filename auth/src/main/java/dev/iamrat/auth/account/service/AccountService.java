package dev.iamrat.auth.account.service;

import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.entity.AccountStatus;
import dev.iamrat.auth.account.entity.Role;
import dev.iamrat.auth.account.repository.AccountRepository;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public Account createGeneralAccount(String username, String rawPassword, String email, String nickname) {
        Account account = Account.builder()
            .username(username)
            .password(passwordEncoder.encode(rawPassword))
            .email(email)
            .nickname(nickname)
            .provider("LOCAL")
            .providerId(null)
            .build();

        account.addRole(Role.USER);

        return accountRepository.save(account);
    }

    @Transactional
    public Account createOAuthAccount(String provider, String providerId, String email, String nickname) {
        Account account = Account.builder()
            .username(provider.toLowerCase(Locale.ROOT) + "_" + providerId)
            .password(null)
            .email(email)
            .nickname(nickname)
            .provider(provider)
            .providerId(providerId)
            .build();

        account.addRole(Role.USER);

        return accountRepository.save(account);
    }

    public Account findWithRolesById(Long accountId) {
        return accountRepository.findWithRolesById(accountId)
            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }
    
    public boolean existsByUsername(String username) {
        return accountRepository.existsByUsername(username);
    }

    @Transactional
    public void updateNickname(Long accountId, String nickname) {
        Account account = findWithRolesById(accountId);
        ensureActive(account);

        if (accountRepository.existsByNickname(nickname)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_NICKNAME);
        }

        account.updateNickname(nickname);
    }

    @Transactional
    public void updatePassword(Long accountId, String currentPassword, String newPassword) {
        Account account = findWithRolesById(accountId);
        ensureActive(account);

        if (!account.isLocalAccount()) {
            throw new CustomException(AuthErrorCode.OAUTH_PASSWORD_UPDATE_NOT_ALLOWED);
        }

        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD);
        }

        account.updatePassword(newPassword, passwordEncoder);
    }

    @Transactional
    public void updateStatus(Long accountId, AccountStatus status) {
        if (status == null) {
            throw new CustomException(AuthErrorCode.INVALID_ACCOUNT_STATUS);
        }

        Account account = findWithRolesById(accountId);
        account.updateStatus(status);
    }

    private void ensureActive(Account account) {
        if (!account.isActive()) {
            throw new CustomException(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }
}
