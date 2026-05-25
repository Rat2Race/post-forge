package dev.iamrat.auth.account.application;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.account.AccountProfile;
import dev.iamrat.core.account.AccountProfileReader;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AccountProfileReaderAdapter implements AccountProfileReader {

    private final AccountStore accountStore;

    @Override
    @Transactional(readOnly = true)
    public AccountProfile getProfile(Long accountId) {
        Account account = accountStore.findById(accountId)
            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        if (!account.isActive()) {
            throw new CustomException(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        return new AccountProfile(account.getId(), account.getNickname());
    }
}
