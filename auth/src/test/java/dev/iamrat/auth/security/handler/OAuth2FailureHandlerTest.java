package dev.iamrat.auth.security.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class OAuth2FailureHandlerTest {

    private OAuth2FailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        OAuth2RedirectProperties oAuth2RedirectProperties = new OAuth2RedirectProperties();
        oAuth2RedirectProperties.setRedirectUrl("https://front.example");
        failureHandler = new OAuth2FailureHandler(oAuth2RedirectProperties);
    }

    @Test
    @DisplayName("OAuth2 인증 실패 시 오류 메시지를 인코딩해 프론트 콜백 URL로 리다이렉트한다")
    void onAuthenticationFailure_redirectsWithEncodedErrorMessage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
            new MockHttpServletRequest(),
            response,
            new BadCredentialsException("invalid provider")
        );

        assertThat(response.getRedirectedUrl())
            .isEqualTo("https://front.example/oauth2/callback?error=invalid+provider");
    }
}
