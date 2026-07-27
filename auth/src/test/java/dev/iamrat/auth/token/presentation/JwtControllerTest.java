package dev.iamrat.auth.token.presentation;

import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.auth.token.application.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("webmvc")
@WebMvcTest(JwtController.class)
@AutoConfigureMockMvc(addFilters = false)
class JwtControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    CookieProvider cookieProvider;

    @Nested
    @DisplayName("토큰 재발급 성공")
    class ReissueSuccess {

        @Test
        @DisplayName("유효한 리프레시 토큰 쿠키가 있으면 200과 새 액세스 토큰을 반환한다")
        void reissue_validRefreshTokenCookie_returns200() throws Exception {
            TokenIssueResult tokenIssueResult = TokenIssueResult.builder()
                .grantType("Bearer")
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

            given(cookieProvider.extractRefreshToken(any()))
                .willReturn("valid-refresh-token");
            given(tokenService.reissueToken("valid-refresh-token")).willReturn(tokenIssueResult);

            mockMvc.perform(post("/api/auth/token/reissue")
                    .cookie(new Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
        }
    }
}
