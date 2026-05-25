package dev.iamrat.support.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class AuditorAwareConfigTest {

    private final AuditorAware<String> auditorAware = new AuditorAwareConfig().auditorProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 정보가 없으면 감사자를 비운다")
    void auditorProvider_returnsEmptyWithoutAuthentication() {
        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("anonymousUser 인증은 감사자를 비운다")
    void auditorProvider_returnsEmptyForAnonymousUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(authenticatedToken("anonymousUser"));

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("UserDetails principal은 username을 감사자로 사용한다")
    void auditorProvider_usesUserDetailsUsername() {
        User user = new User("auditor", "password", List.of());
        SecurityContextHolder.getContext()
            .setAuthentication(authenticatedToken(user));

        assertThat(auditorAware.getCurrentAuditor()).contains("auditor");
    }

    @Test
    @DisplayName("UserDetails가 아닌 인증은 authentication name을 감사자로 사용한다")
    void auditorProvider_usesAuthenticationNameForOtherPrincipal() {
        SecurityContextHolder.getContext()
            .setAuthentication(authenticatedToken("batch-runner"));

        assertThat(auditorAware.getCurrentAuditor()).contains("batch-runner");
    }

    private static UsernamePasswordAuthenticationToken authenticatedToken(Object principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
