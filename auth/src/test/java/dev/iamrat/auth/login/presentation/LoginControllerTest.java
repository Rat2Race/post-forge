package dev.iamrat.auth.login.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.support.web.TestExceptionResponseHandler;
import dev.iamrat.auth.security.infrastructure.principal.AuthenticatedAccount;
import dev.iamrat.auth.login.application.LoginService;
import dev.iamrat.auth.login.presentation.dto.LoginRequest;
import dev.iamrat.auth.security.infrastructure.handler.SecurityExceptionHandler;
import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.auth.token.presentation.CookieProvider;
import java.util.List;
import java.util.stream.Stream;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("webmvc")
@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TestExceptionResponseHandler.class, SecurityExceptionHandler.class})
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
    
    private TokenIssueResult createTokenResponse() {
        return TokenIssueResult.builder()
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
            TokenIssueResult tokenResponse = createTokenResponse();
            given(loginService.login(anyString(), anyString(), anyString())).willReturn(tokenResponse);
            
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
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
                .andExpect(jsonPath("$.validation." + fieldName).exists());
        }
        
        @Test
        @DisplayName("요청 바디가 없으면 400을 반환한다")
        void login_missingBody_returns400() throws Exception {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }
        
        static Stream<Arguments> loginBusinessExceptions() {
            return Stream.of(
                Arguments.of("username이 빈 값이면 400을 반환한다", "username", new LoginRequest("", "Test1234!")),
                Arguments.of("username이 4자 미만이면 400을 반환한다", "username", new LoginRequest("abc", "Test1234!")),
                Arguments.of("username에 특수문자가 포함되면 400을 반환한다", "username", new LoginRequest("testuser1*^_^*", "Test1234!")),
                Arguments.of("비밀번호가 빈 값이면 400을 반환한다", "password", new LoginRequest("testuser1", ""))
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
            given(loginService.login(anyString(), anyString(), anyString()))
                .willThrow(new BadCredentialsException("Bad credentials"));
            
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("로그인 보호 정책에 걸리면 429를 반환한다")
        void login_tooManyAttempts_returns429() throws Exception {
            LoginRequest request = createValidLoginRequest();
            given(loginService.login(anyString(), anyString(), anyString()))
                .willThrow(new CustomException(CommonErrorCode.TOO_MANY_REQUESTS));

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"));
        }
    }
    
    @Nested
    @DisplayName("로그아웃")
    class Logout {
        
        @Test
        @DisplayName("인증된 사용자가 로그아웃하면 200을 반환한다")
        void logout_authenticatedUser_returns200() throws Exception {
            willDoNothing().given(loginService).logout(anyLong());
            SecurityContextHolder.getContext().setAuthentication(userAuthentication());

            try {
                mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    private UsernamePasswordAuthenticationToken userAuthentication() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return UsernamePasswordAuthenticationToken.authenticated(
            new AuthenticatedAccount(1L),
            null,
            authorities
        );
    }
}
