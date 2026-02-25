package dev.iamrat.integration.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class MockMemberSecurityContextFactory implements WithSecurityContextFactory<WithMockMember> {
    
    @Override
    public SecurityContext createSecurityContext(WithMockMember annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        
        String memberId = annotation.memberId();
        String role = annotation.role();
        
        UserDetails principle = User.builder()
            .username(memberId)
            .password("dummy_password")
            .roles(role)
            .build();
        
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principle,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
        
        context.setAuthentication(authentication);
        return context;
    }
}
