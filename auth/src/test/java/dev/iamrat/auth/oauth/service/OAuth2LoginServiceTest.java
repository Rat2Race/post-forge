package dev.iamrat.auth.oauth.service;

import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.entity.Role;
import dev.iamrat.auth.account.service.AccountService;
import dev.iamrat.auth.token.dto.JwtResponse;
import dev.iamrat.auth.token.provider.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OAuth2LoginServiceTest {

    @Mock
    private OAuth2CodeService oAuth2CodeService;

    @Mock
    private AccountService accountService;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private OAuth2LoginService oAuth2LoginService;

    @Test
    @DisplayName("유효한 교환 코드면 회원 정보를 바탕으로 JWT를 발급한다")
    void exchange_validCode_issuesJwt() {
        Account account = account("oauth-user", "tester");
        JwtResponse jwtResponse = JwtResponse.builder()
            .grantType("Bearer")
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .build();

        given(oAuth2CodeService.exchangeCode("exchange-code")).willReturn("oauth-user");
        given(accountService.findByUserId("oauth-user")).willReturn(account);
        given(jwtProvider.createToken(
            account.getUserId(),
            account.getNickname(),
            account.getAuthorities()
        )).willReturn(jwtResponse);

        JwtResponse result = oAuth2LoginService.exchange("exchange-code");

        assertThat(result).isEqualTo(jwtResponse);
        verify(oAuth2CodeService).exchangeCode("exchange-code");
        verify(accountService).findByUserId("oauth-user");
    }

    @Test
    @DisplayName("빈 코드는 잘못된 입력으로 처리한다")
    void exchange_blankCode_throwsInvalidInput() {
        assertThatThrownBy(() -> oAuth2LoginService.exchange("   "))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("잘못된 입력");
    }

    private Account account(String userId, String nickname) {
        Account account = Account.builder()
            .userId(userId)
            .nickname(nickname)
            .email(userId + "@test.com")
            .provider("GOOGLE")
            .providerId("google-user-123")
            .build();
        account.addRole(Role.USER);
        return account;
    }
}
