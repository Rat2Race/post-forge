package dev.iamrat.auth.login.application;

import dev.iamrat.auth.security.infrastructure.principal.CustomUserDetails;
import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.auth.token.application.TokenService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private LoginService loginService;

    @Test
    @DisplayName("로그인 성공 시 토큰을 발급한다")
    void login_success_issuesToken() {
        Authentication authentication = authentication();
        TokenIssueResult tokenIssueResult = TokenIssueResult.builder()
            .grantType("Bearer")
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .build();

        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(tokenService.createToken(eq(1L), any())).willReturn(tokenIssueResult);

        TokenIssueResult result = loginService.login("testuser1", "Test1234!", "127.0.0.1");

        assertThat(result).isEqualTo(tokenIssueResult);
    }

    @Test
    @DisplayName("자격 증명이 틀리면 인증 예외를 유지한다")
    void login_badCredentials_keepsAuthenticationException() {
        given(authenticationManager.authenticate(any()))
            .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> loginService.login("testuser1", "Test1234!", "127.0.0.1"))
            .isInstanceOf(BadCredentialsException.class);

        verify(tokenService, org.mockito.Mockito.never()).createToken(any(), any());
    }

    private Authentication authentication() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return UsernamePasswordAuthenticationToken.authenticated(
            new CustomUserDetails(1L, "testuser1", "", authorities),
            null,
            authorities
        );
    }
}
