package dev.iamrat.board.integration.security;

import dev.iamrat.core.account.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class MockAccountSecurityContextFactory implements WithSecurityContextFactory<WithMockAccount> {

    @Override
    public SecurityContext createSecurityContext(WithMockAccount annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        Long accountId = Long.valueOf(annotation.accountId());
        String role = annotation.role();

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        UserPrincipal principal = new MockUserPrincipal(accountId);

        UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);

        context.setAuthentication(authentication);
        return context;
    }

    private record MockUserPrincipal(Long accountId) implements UserPrincipal {
        @Override
        public Long getAccountId() { return accountId; }
    }
}
