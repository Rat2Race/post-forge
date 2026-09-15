package dev.iamrat.auth.security.infrastructure.handler;

import dev.iamrat.auth.oauth.application.OAuth2CodeService;
import dev.iamrat.auth.security.infrastructure.principal.AuthenticatedAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OAuth2RedirectTargetTest {

    private static final String REDIRECT_URL = "https://front.example/oauth2/callback";

    @Mock
    private OAuth2CodeService oAuth2CodeService;

    private OAuth2SuccessHandler successHandler;
    private OAuth2FailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        OAuth2RedirectProperties properties = new OAuth2RedirectProperties();
        properties.setRedirectUrl(REDIRECT_URL);

        successHandler = new OAuth2SuccessHandler(oAuth2CodeService, properties);
        failureHandler = new OAuth2FailureHandler(properties);
    }

    @Test
    @DisplayName("성공과 실패가 같은 redirect-url로 돌아간다 - query만 다르다")
    void successAndFailure_shareTheSameRedirectTarget() throws Exception {
        given(oAuth2CodeService.createCode(42L)).willReturn("oauth-code-42");

        MockHttpServletResponse successResponse = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
            new MockHttpServletRequest(),
            successResponse,
            new TestingAuthenticationToken(new AuthenticatedAccount(42L), null)
        );

        MockHttpServletResponse failureResponse = new MockHttpServletResponse();
        failureHandler.onAuthenticationFailure(
            new MockHttpServletRequest(),
            failureResponse,
            new BadCredentialsException("invalid provider")
        );

        assertThat(successResponse.getRedirectedUrl()).isEqualTo(REDIRECT_URL + "?code=oauth-code-42");
        assertThat(failureResponse.getRedirectedUrl()).isEqualTo(REDIRECT_URL + "?error=invalid+provider");
    }

    @Test
    @DisplayName("오류 메시지가 없어도 실패 리다이렉트는 동작한다")
    void failureWithoutMessage_stillRedirects() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
            new MockHttpServletRequest(),
            response,
            new BadCredentialsException(null)
        );

        assertThat(response.getRedirectedUrl()).startsWith(REDIRECT_URL + "?error=");
    }
}
