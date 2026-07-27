package dev.iamrat.auth.oauth.presentation;

import dev.iamrat.auth.oauth.application.OAuth2LoginService;
import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.auth.token.presentation.CookieProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("webmvc")
@WebMvcTest(controllers = OAuth2Controller.class)
@AutoConfigureMockMvc(addFilters = false)
class OAuth2ControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OAuth2LoginService oAuth2LoginService;

    @MockitoBean
    CookieProvider cookieProvider;

    @Nested
    @DisplayName("OAuth2 코드 교환 성공")
    class ExchangeSuccess {

        @Test
        @DisplayName("신규 OAuth2 경로에서 액세스 토큰을 반환한다")
        void exchange_newRoute_returnsAccessToken() throws Exception {
            TokenIssueResult tokenIssueResult = TokenIssueResult.builder()
                .grantType("Bearer")
                .accessToken("oauth-access-token")
                .refreshToken("oauth-refresh-token")
                .build();

            given(oAuth2LoginService.exchange("exchange-code"))
                .willReturn(tokenIssueResult);

            mockMvc.perform(post("/api/auth/oauth2/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "code": "exchange-code"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(jsonPath("$.grantType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("oauth-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

            verify(cookieProvider).addRefreshTokenCookie(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("oauth-refresh-token")
            );
        }

    }

    @Nested
    @DisplayName("OAuth2 코드 교환 실패")
    class ExchangeFailure {

        @Test
        @DisplayName("code가 비어 있으면 validation error를 반환한다")
        void exchange_blankCode_returnsValidationError() throws Exception {
            mockMvc.perform(post("/api/auth/oauth2/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "code": " "
                        }
                        """))
                .andExpect(status().isBadRequest());
        }
    }
}
