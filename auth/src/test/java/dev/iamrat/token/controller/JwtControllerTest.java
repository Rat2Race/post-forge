package dev.iamrat.token.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.global.exception.GlobalExceptionHandler;
import dev.iamrat.token.dto.JwtReissueRequest;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.provider.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("webmvc")
@WebMvcTest(JwtController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class JwtControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JwtProvider jwtProvider;

    @Nested
    @DisplayName("토큰 재발급 성공")
    class ReissueSuccess {

        @Test
        @DisplayName("유효한 리프레시 토큰이면 200과 새 토큰을 반환한다")
        void reissue_validRefreshToken_returns200() throws Exception {
            JwtReissueRequest request = new JwtReissueRequest("valid-refresh-token");
            JwtResponse response = JwtResponse.builder()
                .grantType("Bearer")
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

            given(jwtProvider.reissueToken(anyString())).willReturn(response);

            mockMvc.perform(post("/auth/token/reissue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andDo(print());
        }
    }

    @Nested
    @DisplayName("토큰 재발급 실패 - 입력값 검증")
    class ReissueValidationFail {

        @Test
        @DisplayName("리프레시 토큰이 빈 값이면 400을 반환한다")
        void reissue_blankRefreshToken_returns400() throws Exception {
            JwtReissueRequest request = new JwtReissueRequest("");

            mockMvc.perform(post("/auth/token/reissue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validation.refreshToken").exists())
                .andDo(print());
        }

        @Test
        @DisplayName("요청 바디가 없으면 400을 반환한다")
        void reissue_missingBody_returns400() throws Exception {
            mockMvc.perform(post("/auth/token/reissue")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andDo(print());
        }
    }

    @Nested
    @DisplayName("토큰 재발급 실패 - 비즈니스 로직")
    class ReissueBusinessFail {

        @Test
        @DisplayName("유효하지 않은 토큰이면 401을 반환한다")
        void reissue_invalidRefreshToken_returns401() throws Exception {
            JwtReissueRequest request = new JwtReissueRequest("invalid-refresh-token");
            given(jwtProvider.reissueToken(anyString()))
                .willThrow(new CustomException(ErrorCode.INVALID_TOKEN));

            mockMvc.perform(post("/auth/token/reissue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"))
                .andDo(print());
        }

        @Test
        @DisplayName("만료된 토큰이면 401을 반환한다")
        void reissue_mismatchRefreshToken_return401() throws Exception {
            JwtReissueRequest request = new JwtReissueRequest("expired-refresh-token");
            given(jwtProvider.reissueToken(anyString()))
                .willThrow(new CustomException(ErrorCode.EXPIRED_TOKEN));

            mockMvc.perform(post("/auth/token/reissue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("EXPIRED_TOKEN"))
                .andDo(print());
        }
    }
}
