package dev.iamrat.board.integration.security;

import dev.iamrat.core.global.security.UserPrincipal;
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

        String accountId = annotation.accountId();
        String role = annotation.role();

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        UserPrincipal principal = new MockUserPrincipal(accountId, "테스트유저");

        UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);

        context.setAuthentication(authentication);
        return context;
    }

    private record MockUserPrincipal(String userId, String nickname) implements UserPrincipal {
        @Override
        public String getUserId() { return userId; }
        @Override
        public String getNickname() { return nickname; }
    }
}
