package dev.iamrat.support.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.core.account.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class AuditorAwareConfigTest {

    private final AuditorAware<String> auditorAware = new AuditorAwareConfig().auditorProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 정보가 없으면 시스템 감사자를 사용한다")
    void auditorProvider_usesSystemWithoutAuthentication() {
        assertThat(auditorAware.getCurrentAuditor()).contains("SYSTEM");
    }

    @Test
    @DisplayName("익명 인증이면 시스템 감사자를 사용한다")
    void auditorProvider_usesSystemForAnonymousAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        ));

        assertThat(auditorAware.getCurrentAuditor()).contains("SYSTEM");
    }

    @Test
    @DisplayName("공통 사용자 principal은 계정 ID를 감사자로 사용한다")
    void auditorProvider_usesAccountIdForUserPrincipal() {
        UserPrincipal principal = () -> 42L;
        SecurityContextHolder.getContext().setAuthentication(authenticatedToken(principal));

        assertThat(auditorAware.getCurrentAuditor()).contains("42");
    }

    @Test
    @DisplayName("그 외 인증은 Spring Security가 제공하는 인증 이름을 사용한다")
    void auditorProvider_usesAuthenticationNameForOtherPrincipal() {
        User user = new User("auditor", "password", List.of());
        SecurityContextHolder.getContext()
            .setAuthentication(authenticatedToken(user));

        assertThat(auditorAware.getCurrentAuditor()).contains("auditor");
    }

    @Test
    @DisplayName("auditing 시간은 주입된 Clock을 사용한다")
    void auditingDateTimeProvider_usesInjectedClock() {
        Clock clock = Clock.fixed(
            Instant.parse("2026-06-21T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
        );

        assertThat(new AuditorAwareConfig().auditingDateTimeProvider(clock).getNow())
            .contains(LocalDateTime.of(2026, 6, 21, 9, 0));
    }

    private static UsernamePasswordAuthenticationToken authenticatedToken(Object principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
