package dev.iamrat.auth.token.application;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountStatus;
import dev.iamrat.auth.account.domain.AccountRole;
import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.security.infrastructure.principal.AuthenticatedAccount;
import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.core.account.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private AccountQueryService accountQueryService;

    @InjectMocks
    private TokenService tokenService;

    private static final Long ACCOUNT_ID = 1L;
    private static final String USERNAME = "testuser1";

    @Nested
    @DisplayName("토큰 생성")
    class CreateToken {

        @Test
        @DisplayName("사용자 식별자와 권한으로 토큰을 생성한다")
        void createToken_validUserContext_returnsTokenIssueResult() {
            given(tokenIssuer.generateAccessToken(any(), any())).willReturn("access-token");
            given(tokenIssuer.generateRefreshToken(any())).willReturn("refresh-token");

            TokenIssueResult result = tokenService.createToken(
                ACCOUNT_ID,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );

            assertThat(result.grantType()).isEqualTo("Bearer");
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            verify(tokenIssuer).generateAccessToken(
                eq(ACCOUNT_ID),
                eq(List.of("ROLE_USER"))
            );
            verify(tokenIssuer).generateRefreshToken(eq(ACCOUNT_ID));
            verify(refreshTokenStore).save(eq(ACCOUNT_ID), eq("refresh-token"));
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class ReissueToken {

        @Test
        @DisplayName("유효한 리프레시 토큰이면 새 토큰을 발급한다")
        void reissueToken_validRefreshToken_returnsTokenIssueResult() {
            given(tokenIssuer.parse("old-refresh-token"))
                .willReturn(new TokenClaims(String.valueOf(ACCOUNT_ID), List.of()));

            Account account = Account.builder()
                .id(ACCOUNT_ID)
                .username(USERNAME)
                .email("test@test.com")
                .nickname("tester")
                .roles(Set.of(AccountRole.USER))
                .build();

            given(accountQueryService.findWithRolesById(ACCOUNT_ID)).willReturn(account);

            given(tokenIssuer.generateAccessToken(eq(ACCOUNT_ID), any())).willReturn("new-access-token");
            given(tokenIssuer.generateRefreshToken(ACCOUNT_ID)).willReturn("new-refresh-token");

            TokenIssueResult result = tokenService.reissueToken("old-refresh-token");

            assertThat(result.grantType()).isEqualTo("Bearer");
            assertThat(result.accessToken()).isEqualTo("new-access-token");
            assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
            verify(refreshTokenStore).validate(ACCOUNT_ID, "old-refresh-token");
            verify(refreshTokenStore).save(eq(ACCOUNT_ID), eq("new-refresh-token"));
        }

        @Test
        @DisplayName("저장된 토큰과 다르면 INVALID_TOKEN 예외를 던진다")
        void reissueToken_tokenMismatch_throwsInvalidToken() {
            given(tokenIssuer.parse("wrong-refresh-token"))
                .willReturn(new TokenClaims(String.valueOf(ACCOUNT_ID), List.of()));

            doThrow(new CustomException(AuthErrorCode.INVALID_TOKEN))
                .when(refreshTokenStore).validate(ACCOUNT_ID, "wrong-refresh-token");

            assertThatThrownBy(() -> tokenService.reissueToken("wrong-refresh-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("비활성 계정이면 리프레시 토큰으로 새 토큰을 발급하지 않는다")
        void reissueToken_inactiveAccount_throwsAccountNotActive() {
            given(tokenIssuer.parse("old-refresh-token"))
                .willReturn(new TokenClaims(String.valueOf(ACCOUNT_ID), List.of()));

            Account account = Account.builder()
                .id(ACCOUNT_ID)
                .username(USERNAME)
                .email("test@test.com")
                .nickname("tester")
                .status(AccountStatus.SUSPENDED)
                .roles(Set.of(AccountRole.USER))
                .build();

            given(accountQueryService.findWithRolesById(ACCOUNT_ID)).willReturn(account);

            assertThatThrownBy(() -> tokenService.reissueToken("old-refresh-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode()).isEqualTo(AuthErrorCode.ACCOUNT_NOT_ACTIVE));

            verify(tokenIssuer, never()).generateAccessToken(any(), any());
            verify(tokenIssuer, never()).generateRefreshToken(any());
        }

        @Test
        @DisplayName("파싱에 실패하면 INVALID_TOKEN 예외를 던진다")
        void reissueToken_parsingFails_throwsInvalidToken() {
            given(tokenIssuer.parse("malformed-token"))
                .willThrow(new CustomException(AuthErrorCode.INVALID_TOKEN));

            assertThatThrownBy(() -> tokenService.reissueToken("malformed-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN));
        }
    }

    @Nested
    @DisplayName("토큰 삭제")
    class DeleteToken {

        @Test
        @DisplayName("accountId로 리프레시 토큰을 삭제한다")
        void deleteToken_accountIdProvided_deletesRefreshToken() {
            tokenService.deleteToken(ACCOUNT_ID);

            verify(refreshTokenStore).delete(eq(ACCOUNT_ID));
        }
    }

    @Nested
    @DisplayName("Authentication 파서")
    class ResolveAuthentication {

        @Test
        @DisplayName("유효한 토큰이면 Authentication을 반환한다")
        void resolveAuthentication_validToken_returnsAuthentication() {
            given(tokenIssuer.parse("valid-token"))
                .willReturn(new TokenClaims(String.valueOf(ACCOUNT_ID), List.of("ROLE_USER")));

            Authentication result = tokenService.resolveAuthentication("valid-token");

            assertThat(result.getName()).isEqualTo(String.valueOf(ACCOUNT_ID));
            assertThat(result.getAuthorities()).hasSize(1);
            assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
            assertThat(result.getCredentials()).isNull();
            assertThat(result.getPrincipal()).isInstanceOf(AuthenticatedAccount.class);

            UserPrincipal principal = (UserPrincipal) result.getPrincipal();
            assertThat(principal.getAccountId()).isEqualTo(ACCOUNT_ID);
        }

        @Test
        @DisplayName("subject가 빈 값이면 예외가 발생한다")
        void resolveAuthentication_blankSubject_throwsInvalidToken() {
            given(tokenIssuer.parse("token-without-subject"))
                .willReturn(new TokenClaims("", List.of()));

            assertThatThrownBy(() -> tokenService.resolveAuthentication("token-without-subject"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_TOKEN));
        }
    }
}
