package dev.iamrat.auth.account.application;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountPolicy;
import dev.iamrat.auth.account.domain.AccountStatus;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.support.normalizer.EmailNormalizer;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountCommandService {
    private final AccountStore accountStore;
    private final PasswordEncoder passwordEncoder;
    private final AccountPolicy accountPolicy = new AccountPolicy();

    @Transactional
    public Account createGeneralAccount(String username, String rawPassword, String email, String nickname) {
        Account account = Account.createLocal(
            username,
            passwordEncoder.encode(rawPassword),
            EmailNormalizer.normalize(email),
            nickname
        );

        return accountStore.saveAndFlush(account);
    }

    private Account findWithRolesById(Long accountId) {
        return accountStore.findWithRolesById(accountId)
            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void updateNickname(Long accountId, String nickname) {
        Account account = findWithRolesById(accountId);
        accountPolicy.requireActive(account);

        if (accountStore.existsByNickname(nickname)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_NICKNAME);
        }

        account.updateNickname(nickname);
        accountStore.flush();
    }

    @Transactional
    public void updatePassword(Long accountId, String currentPassword, String newPassword) {
        Account account = findWithRolesById(accountId);
        accountPolicy.requireActive(account);

        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD);
        }

        account.updatePassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void updateStatus(Long accountId, AccountStatus status) {
        accountPolicy.requireStatus(status);

        Account account = findWithRolesById(accountId);
        account.updateStatus(status);
    }
}
