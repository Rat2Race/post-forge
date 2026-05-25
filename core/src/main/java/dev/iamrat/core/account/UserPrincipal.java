package dev.iamrat.core.account;

import java.security.Principal;

public interface UserPrincipal extends Principal {
    Long getAccountId();

    @Override
    default String getName() {
        return String.valueOf(getAccountId());
    }
}
