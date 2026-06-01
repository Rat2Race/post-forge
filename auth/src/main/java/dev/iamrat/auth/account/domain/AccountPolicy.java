package dev.iamrat.auth.account.domain;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;

public class AccountPolicy {

    public void requireActive(Account account) {
        if (!account.isActive()) {
            throw new CustomException(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    public void requireStatus(AccountStatus status) {
        if (status == null) {
            throw new CustomException(AuthErrorCode.INVALID_ACCOUNT_STATUS);
        }
    }
}
