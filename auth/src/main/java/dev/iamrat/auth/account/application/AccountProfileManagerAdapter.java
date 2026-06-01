package dev.iamrat.auth.account.application;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountPolicy;
import dev.iamrat.auth.account.domain.AccountRole;
import dev.iamrat.core.account.AccountProfileDetails;
import dev.iamrat.core.account.AccountProfileManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AccountProfileManagerAdapter implements AccountProfileManager {

    private final AccountQueryService accountQueryService;
    private final AccountCommandService accountCommandService;
    private final AccountPolicy accountPolicy = new AccountPolicy();

    @Override
    @Transactional(readOnly = true)
    public AccountProfileDetails getProfile(Long accountId) {
        Account account = accountQueryService.findWithRolesById(accountId);
        accountPolicy.requireActive(account);
        return toDetails(account);
    }

    @Override
    public void updateNickname(Long accountId, String nickname) {
        accountCommandService.updateNickname(accountId, nickname);
    }

    @Override
    public void updatePassword(Long accountId, String currentPassword, String newPassword) {
        accountCommandService.updatePassword(accountId, currentPassword, newPassword);
    }

    private AccountProfileDetails toDetails(Account account) {
        return new AccountProfileDetails(
            account.getId(),
            account.getUsername(),
            account.getEmail(),
            account.getNickname(),
            account.getRoles().stream()
                .map(AccountRole::getValue)
                .sorted()
                .toList(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }
}
