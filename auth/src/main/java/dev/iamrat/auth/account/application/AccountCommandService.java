package dev.iamrat.auth.account.application;

import dev.iamrat.auth.account.domain.AccountRepository;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountPolicy;
import dev.iamrat.auth.account.domain.AccountRole;
import dev.iamrat.auth.account.domain.AccountStatus;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.support.normalizer.EmailNormalizer;
import dev.iamrat.auth.token.application.RefreshTokenStore;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.core.global.error.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountCommandService {
    private final AccountRepository accountRepository;
    private final AccountQueryService accountQueryService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;
    private final AccountPolicy accountPolicy = new AccountPolicy();

    @Transactional
    public Account createGeneralAccount(String username, String rawPassword, String email, String nickname) {
        Account account = Account.createLocal(
            username,
            passwordEncoder.encode(rawPassword),
            EmailNormalizer.normalize(email),
            nickname
        );

        return accountRepository.saveAndFlush(account);
    }

    @Transactional
    public Account createOAuthAccount(String provider, String providerId, String email, String nickname) {
        Account account = Account.createOAuth(
            provider,
            providerId,
            EmailNormalizer.normalize(email),
            nickname
        );

        return accountRepository.saveAndFlush(account);
    }

    @Transactional
    public void updateNickname(Long accountId, String nickname) {
        Account account = findWithRolesById(accountId);
        accountPolicy.requireActive(account);

        if (accountRepository.existsByNickname(nickname)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_NICKNAME);
        }

        account.updateNickname(nickname);
        accountRepository.flush();
    }

    @Transactional
    public void updatePassword(Long accountId, String currentPassword, String newPassword) {
        Account account = findWithRolesById(accountId);
        accountPolicy.requireActive(account);
        accountPolicy.requireLocalAccount(account);

        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD);
        }

        account.updatePassword(passwordEncoder.encode(newPassword));
        refreshTokenStore.delete(accountId);
    }

    @Transactional
    public void updateStatus(Long accountId, AccountStatus status) {
        accountPolicy.requireStatus(status);

        Account account = findWithRolesById(accountId);
        account.updateStatus(status);
    }

    @Transactional
    public void grantAdminRole(Long actorId, Long targetId) {
        if (actorId == null || targetId == null || targetId <= 0 || actorId.equals(targetId)) {
            throw new CustomException(CommonErrorCode.ACCESS_DENIED);
        }

        Account actor = accountQueryService.findWithRolesById(actorId)
            .orElseThrow(() -> new CustomException(CommonErrorCode.ACCESS_DENIED));
        if (!actor.isActive() || !actor.getRoles().contains(AccountRole.ADMIN)) {
            throw new CustomException(CommonErrorCode.ACCESS_DENIED);
        }

        Account target = findWithRolesById(targetId);
        accountPolicy.requireActive(target);
        if (!target.getRoles().contains(AccountRole.ADMIN)) {
            target.addRole(AccountRole.ADMIN);
            accountRepository.flush();
        }
    }

    private Account findWithRolesById(Long accountId) {
        return accountQueryService.findWithRolesById(accountId)
            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }
}
