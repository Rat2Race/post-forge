package dev.iamrat.auth.security.infrastructure.principal;

import dev.iamrat.core.account.UserPrincipal;

public record AuthenticatedAccount(Long accountId) implements UserPrincipal {
    @Override
    public Long getAccountId() {
        return accountId;
    }
}
