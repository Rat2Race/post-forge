package dev.iamrat.login.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.global.exception.GlobalExceptionHandler;
import dev.iamrat.login.dto.LoginRequest;
import dev.iamrat.login.service.LoginService;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.provider.CookieProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("webmvc")
@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LoginControllerTest {
    
    @Autowired
    MockMvc mockMvc;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @MockitoBean
    LoginService loginService;

    @MockitoBean
    CookieProvider cookieProvider;
    
    private LoginRequest createValidLoginRequest() {
        return new LoginRequest("testuser1", "Test1234!");
    }
    
    private JwtResponse createTokenResponse() {
        return JwtResponse.builder()
            .grantType("Bearer")
            .accessToken("mock-access-token")
            .refreshToken("mock-refresh-token")
            .build();
    }
    
    @Nested
    @DisplayName("로그인 성공")
    class LoginSuccess {
        
        @Test
        @DisplayName("유효한 자격 증명이면 200과 토큰을 반환한다")
        void login_validCredentials_returns200() throws Exception {
            LoginRequest request = createValidLoginRequest();
            JwtResponse tokenResponse = createTokenResponse();
            given(loginService.login(any(LoginRequest.class))).willReturn(tokenResponse);
            
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andDo(print());
        }
    }
    
    @Nested
    @DisplayName("로그인 실패 - 입력값 검증")
    class LoginValidationFail {
        
        @ParameterizedTest(name = "{0}")
        @MethodSource("loginBusinessExceptions")
        void login_invalidRequest_returns400(String description, String fieldName, LoginRequest request) throws Exception {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validation." + fieldName).exists())
                .andDo(print());
        }
        
        @Test
        @DisplayName("요청 바디가 없으면 400을 반환한다")
        void login_missingBody_returns400() throws Exception {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andDo(print());
        }
        
        static Stream<Arguments> loginBusinessExceptions() {
            return Stream.of(
                Arguments.of("아이디가 빈 값이면 400을 반환한다", "id", new LoginRequest("", "Test1234!")),
                Arguments.of("아이디가 4자 미만이면 400을 반환한다", "id", new LoginRequest("abc", "Test1234!")),
                Arguments.of("아이디에 특수문자가 포함되면 400을 반환한다", "id", new LoginRequest("testuser1*^_^*", "Test1234!")),
                Arguments.of("비밀번호가 빈 값이면 400을 반환한다", "pw",new LoginRequest("testuser1", ""))
            );
        }
    }
    
    @Nested
    @DisplayName("로그인 실패 - 인증")
    class LoginAuthFail {
        
        @Test
        @DisplayName("아이디 또는 비밀번호가 틀리면 401을 반환한다")
        void login_invalidCredentials_returns401() throws Exception {
            LoginRequest request = createValidLoginRequest();
            given(loginService.login(any(LoginRequest.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));
            
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andDo(print());
        }
    }
    
    @Nested
    @DisplayName("로그아웃")
    class Logout {
        
        @Test
        @DisplayName("인증된 사용자가 로그아웃하면 200을 반환한다")
        @WithMockUser(username = "testuser1", roles = {"USER"})
        void logout_authenticatedUser_returns200() throws Exception {
            willDoNothing().given(loginService).logout(anyString());
            
            mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andDo(print());
        }
    }
}