package dev.iamrat.auth.security.principal;

import dev.iamrat.core.account.UserPrincipal;
import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public record CustomOAuth2User(
    Long accountId,
    Map<String, Object> attributes,
    Collection<? extends GrantedAuthority> authorities
) implements OAuth2User, UserPrincipal {

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return String.valueOf(accountId);
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }
}
