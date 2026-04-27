package dev.iamrat.token.controller;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.global.exception.GlobalExceptionHandler;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.provider.CookieProvider;
import dev.iamrat.token.provider.JwtProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("webmvc")
@WebMvcTest(JwtController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class JwtControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtProvider jwtProvider;

    @MockitoBean
    CookieProvider cookieProvider;

    @Nested
    @DisplayName("토큰 재발급 성공")
    class ReissueSuccess {

        @Test
        @DisplayName("유효한 리프레시 토큰 쿠키가 있으면 200과 새 액세스 토큰을 반환한다")
        void reissue_validRefreshTokenCookie_returns200() throws Exception {
            JwtResponse jwtResponse = JwtResponse.builder()
                .grantType("Bearer")
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

            given(cookieProvider.extractRefreshToken(any()))
                .willReturn("valid-refresh-token");
            given(jwtProvider.reissueToken("valid-refresh-token")).willReturn(jwtResponse);

            mockMvc.perform(post("/auth/token/reissue")
                    .cookie(new Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andDo(print());
        }
    }

    @Nested
    @DisplayName("토큰 재발급 실패 - 쿠키 검증")
    class ReissueCookieValidationFail {

        @Test
        @DisplayName("리프레시 토큰 쿠키가 없으면 401을 반환한다")
        void reissue_noCookie_returns401() throws Exception {
            given(cookieProvider.extractRefreshToken(any()))
                .willReturn(null);

            mockMvc.perform(post("/auth/token/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"))
                .andDo(print());
        }

        @Test
        @DisplayName("리프레시 토큰 쿠키가 빈 값이면 401을 반환한다")
        void reissue_blankCookie_returns401() throws Exception {
            given(cookieProvider.extractRefreshToken(any()))
                .willReturn("");

            mockMvc.perform(post("/auth/token/reissue")
                    .cookie(new Cookie("refresh_token", "")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"))
                .andDo(print());
        }
    }

    @Nested
    @DisplayName("토큰 재발급 실패 - 비즈니스 로직")
    class ReissueBusinessFail {

        @Test
        @DisplayName("유효하지 않은 토큰이면 401을 반환한다")
        void reissue_invalidRefreshToken_returns401() throws Exception {
            given(cookieProvider.extractRefreshToken(any()))
                .willReturn("invalid-refresh-token");
            given(jwtProvider.reissueToken(anyString()))
                .willThrow(new CustomException(ErrorCode.INVALID_TOKEN));

            mockMvc.perform(post("/auth/token/reissue")
                    .cookie(new Cookie("refresh_token", "invalid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"))
                .andDo(print());
        }

        @Test
        @DisplayName("만료된 토큰이면 401을 반환한다")
        void reissue_expiredRefreshToken_returns401() throws Exception {
            given(cookieProvider.extractRefreshToken(any()))
                .willReturn("expired-refresh-token");
            given(jwtProvider.reissueToken(anyString()))
                .willThrow(new CustomException(ErrorCode.EXPIRED_TOKEN));

            mockMvc.perform(post("/auth/token/reissue")
                    .cookie(new Cookie("refresh_token", "expired-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("EXPIRED_TOKEN"))
                .andDo(print());
        }
    }
}
