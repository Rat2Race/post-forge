package dev.iamrat.auth.login.application;

import dev.iamrat.auth.security.infrastructure.principal.CustomUserDetails;
import dev.iamrat.auth.login.application.LoginAttemptGuard;
import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.auth.token.application.TokenService;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private LoginAttemptGuard loginAttemptGuard;

    @InjectMocks
    private LoginService loginService;

    @Test
    @DisplayName("로그인 성공 시 보호 정책을 확인하고 실패 카운터를 초기화한다")
    void login_success_clearsFailureCounter() {
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
        verify(loginAttemptGuard).guard("testuser1", "127.0.0.1");
        verify(loginAttemptGuard).clearFailure("testuser1");
        verify(loginAttemptGuard, never()).recordFailure("testuser1");
    }

    @Test
    @DisplayName("자격 증명이 틀리면 실패 카운터를 기록하고 인증 예외를 유지한다")
    void login_badCredentials_recordsFailure() {
        given(authenticationManager.authenticate(any()))
            .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> loginService.login("testuser1", "Test1234!", "127.0.0.1"))
            .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptGuard).guard("testuser1", "127.0.0.1");
        verify(loginAttemptGuard).recordFailure("testuser1");
        verify(loginAttemptGuard, never()).clearFailure("testuser1");
        verify(tokenService, never()).createToken(any(), any());
    }

    @Test
    @DisplayName("실패 누적 한도에 도달하면 429 예외로 전환한다")
    void login_failureLimitExceeded_throwsTooManyRequests() {
        given(authenticationManager.authenticate(any()))
            .willThrow(new BadCredentialsException("Bad credentials"));
        willThrow(new CustomException(CommonErrorCode.TOO_MANY_REQUESTS))
            .given(loginAttemptGuard).recordFailure("testuser1");

        assertThatThrownBy(() -> loginService.login("testuser1", "Test1234!", "127.0.0.1"))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS);

        verify(loginAttemptGuard).guard("testuser1", "127.0.0.1");
        verify(loginAttemptGuard).recordFailure("testuser1");
        verify(tokenService, never()).createToken(any(), any());
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
