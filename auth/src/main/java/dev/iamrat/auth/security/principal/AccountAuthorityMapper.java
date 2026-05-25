package dev.iamrat.auth.security.principal;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountRole;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class AccountAuthorityMapper {

    private AccountAuthorityMapper() {
    }

    public static List<GrantedAuthority> toAuthorities(Account account) {
        return toAuthorities(account.getRoles());
    }

    public static List<GrantedAuthority> toAuthorities(Collection<AccountRole> roles) {
        return toAuthorityNames(roles).stream()
            .map(authorityName -> (GrantedAuthority) new SimpleGrantedAuthority(authorityName))
            .toList();
    }

    public static List<String> toAuthorityNames(Account account) {
        return toAuthorityNames(account.getRoles());
    }

    public static List<String> toAuthorityNames(Collection<AccountRole> roles) {
        return roles.stream()
            .sorted(Comparator.comparing(AccountRole::getValue))
            .map(AccountRole::getValue)
            .toList();
    }
}
