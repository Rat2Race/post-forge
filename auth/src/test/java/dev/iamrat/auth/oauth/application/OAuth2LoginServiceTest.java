package dev.iamrat.auth.oauth.application;

import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountStatus;
import dev.iamrat.auth.account.domain.AccountRole;
import dev.iamrat.auth.security.principal.AccountAuthorityMapper;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.auth.token.application.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OAuth2LoginServiceTest {

    @Mock
    private OAuth2CodeService oAuth2CodeService;

    @Mock
    private AccountQueryService accountQueryService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private OAuth2LoginService oAuth2LoginService;

    @Test
    @DisplayName("유효한 교환 코드면 회원 정보를 바탕으로 JWT를 발급한다")
    void exchange_validCode_issuesJwt() {
        Account account = account("oauth-user", "tester");
        TokenIssueResult tokenIssueResult = TokenIssueResult.builder()
            .grantType("Bearer")
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .build();

        given(oAuth2CodeService.exchangeCode("exchange-code")).willReturn(1L);
        given(accountQueryService.findWithRolesById(1L)).willReturn(account);
        given(tokenService.createToken(account.getId(), AccountAuthorityMapper.toAuthorities(account))).willReturn(tokenIssueResult);

        TokenIssueResult result = oAuth2LoginService.exchange("exchange-code");

        assertThat(result).isEqualTo(tokenIssueResult);
        verify(oAuth2CodeService).exchangeCode("exchange-code");
        verify(accountQueryService).findWithRolesById(1L);
    }

    @Test
    @DisplayName("빈 코드는 잘못된 입력으로 처리한다")
    void exchange_blankCode_throwsInvalidInput() {
        assertThatThrownBy(() -> oAuth2LoginService.exchange("   "))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("잘못된 입력");
    }

    @Test
    @DisplayName("비활성 계정이면 OAuth 교환 코드로 JWT를 발급하지 않는다")
    void exchange_inactiveAccount_throwsAccountNotActive() {
        Account account = account("oauth-user", "tester", AccountStatus.SUSPENDED);

        given(oAuth2CodeService.exchangeCode("exchange-code")).willReturn(1L);
        given(accountQueryService.findWithRolesById(1L)).willReturn(account);

        assertThatThrownBy(() -> oAuth2LoginService.exchange("exchange-code"))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_ACTIVE));

        verify(tokenService, never()).createToken(any(), any());
    }

    private Account account(String username, String nickname) {
        return account(username, nickname, AccountStatus.ACTIVE);
    }

    private Account account(String username, String nickname, AccountStatus status) {
        Account account = Account.builder()
            .id(1L)
            .username(username)
            .nickname(nickname)
            .email(username + "@test.com")
            .provider("GOOGLE")
            .providerId("google-user-123")
            .status(status)
            .build();
        account.addRole(AccountRole.USER);
        return account;
    }
}
