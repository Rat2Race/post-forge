package dev.iamrat.security;

import dev.iamrat.login.dto.CustomUserDetails;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockUserPrincipalSecurityContextFactory
    implements WithSecurityContextFactory<WithMockUserPrincipal> {

    @Override
    public SecurityContext createSecurityContext(WithMockUserPrincipal annotation) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(annotation.roles())
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        CustomUserDetails principal = new CustomUserDetails(
            annotation.userId(), "", annotation.nickname(), authorities
        );

        UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated(principal, "", authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
