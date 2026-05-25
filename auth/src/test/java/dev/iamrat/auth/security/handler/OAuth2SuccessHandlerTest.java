package dev.iamrat.auth.security.handler;

import dev.iamrat.auth.oauth.application.OAuth2CodeService;
import dev.iamrat.auth.security.principal.AuthenticatedAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private OAuth2CodeService oAuth2CodeService;

    private OAuth2RedirectProperties oAuth2RedirectProperties;
    private OAuth2SuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        oAuth2RedirectProperties = new OAuth2RedirectProperties();
        oAuth2RedirectProperties.setRedirectUrl("https://front.example/oauth2/callback");
        successHandler = new OAuth2SuccessHandler(oAuth2CodeService, oAuth2RedirectProperties);
    }

    @Test
    @DisplayName("OAuth2 인증 성공 시 accountId로 코드를 발급하고 프론트 콜백 URL로 리다이렉트한다")
    void onAuthenticationSuccess_redirectsWithIssuedCode() throws Exception {
        given(oAuth2CodeService.createCode(42L)).willReturn("oauth-code-42");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
            new AuthenticatedAccount(42L),
            null
        );

        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
            new MockHttpServletRequest(),
            response,
            authentication
        );

        assertThat(response.getRedirectedUrl())
            .isEqualTo("https://front.example/oauth2/callback?code=oauth-code-42");
        verify(oAuth2CodeService).createCode(42L);
    }
}
