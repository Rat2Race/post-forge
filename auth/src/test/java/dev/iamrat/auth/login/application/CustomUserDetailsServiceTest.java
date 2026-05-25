package dev.iamrat.auth.login.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountStatus;
import dev.iamrat.auth.account.domain.AccountRole;
import dev.iamrat.auth.security.principal.CustomUserDetails;
import dev.iamrat.auth.support.error.AuthErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private AccountQueryService accountQueryService;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("활성 계정이면 Spring Security UserDetails를 반환한다")
    void loadUserByUsername_activeAccount_returnsUserDetails() {
        Account account = account(AccountStatus.ACTIVE);
        given(accountQueryService.findByUsername("testuser1")).willReturn(Optional.of(account));

        UserDetails result = customUserDetailsService.loadUserByUsername("testuser1");

        assertThat(result).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails userDetails = (CustomUserDetails) result;
        assertThat(userDetails.accountId()).isEqualTo(1L);
        assertThat(userDetails.getUsername()).isEqualTo("testuser1");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_USER");
        verify(accountQueryService).findByUsername("testuser1");
    }

    @Test
    @DisplayName("비활성 계정이면 로그인 인증을 실패시킨다")
    void loadUserByUsername_inactiveAccount_throwsDisabledException() {
        Account account = account(AccountStatus.SUSPENDED);
        given(accountQueryService.findByUsername("testuser1")).willReturn(Optional.of(account));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("testuser1"))
            .isInstanceOf(DisabledException.class)
            .hasMessage(AuthErrorCode.ACCOUNT_NOT_ACTIVE.getMessage());
    }

    @Test
    @DisplayName("없는 아이디면 로그인 인증을 실패시킨다")
    void loadUserByUsername_missingAccount_throwsUsernameNotFoundException() {
        given(accountQueryService.findByUsername("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage(AuthErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    private Account account(AccountStatus status) {
        Account account = Account.builder()
            .id(1L)
            .username("testuser1")
            .password("encoded-password")
            .email("test@example.com")
            .nickname("tester")
            .provider("LOCAL")
            .status(status)
            .build();
        account.addRole(AccountRole.USER);
        return account;
    }
}
