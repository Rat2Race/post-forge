package dev.iamrat.register.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.global.exception.GlobalExceptionHandler;
import dev.iamrat.register.dto.RegisterRequest;
import dev.iamrat.register.service.RegisterService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("webmvc")
@WebMvcTest(controllers = RegisterController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RegisterControllerTest {
    
    @Autowired
    MockMvc mockMvc;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @MockitoBean
    RegisterService registerService;
  
    private RegisterRequest createRegisterRequest() {
        return new RegisterRequest("홍길동", "testuser1", "Test1234!", "test@example.com", "길동이");
    }
    
    private RegisterRequest createRequest(String name, String id, String pw, String email, String nickname) {
        return new RegisterRequest(name, id, pw, email, nickname);
    }
    
    @Nested
    @DisplayName("회원가입 성공")
    class RegisterSuccess {
        
        @Test
        @DisplayName("유효한 요청이면 201 Created를 반환한다")
        void register_validRequest_returns201() throws Exception {
            RegisterRequest request = createRegisterRequest();
            given(registerService.register(any(RegisterRequest.class))).willReturn(1L);
            
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(print());
        }
    }
    
    @Nested
    @DisplayName("회원가입 실패 - 입력값 검증")
    class RegisterValidationFail {
        
        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidRegisterRequests")
        void register_invalidRequest_returns400(String description, String fieldName, RegisterRequest registerRequest) throws Exception {
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validation." + fieldName).exists())
                .andDo(print());
        }
        
        static Stream<Arguments> invalidRegisterRequests() {
            return Stream.of(
                Arguments.of("이름이 빈 값이면 400을 반환한다", "name",
                    new RegisterRequest("", "testuser1", "Test1234!", "test@example.com", "길동이")),
                Arguments.of("이름에 특수문자가 포함되면 400을 반환한다", "name",
                    new RegisterRequest("홍길동!", "testuser1", "Test1234!", "test@example.com", "길동이")),
                Arguments.of("아이디가 4자 미만이면 400을 반환한다", "id",
                    new RegisterRequest("홍길동", "abc", "Test1234!", "test@example.com", "길동이")),
                Arguments.of("비밀번호에 대문자/특수문자가 없으면 400을 반환한다", "pw",
                    new RegisterRequest("홍길동", "testuser1", "test1234", "test@example.com", "길동이")),
                Arguments.of("비밀번호가 8자 미만이면 400을 반환한다", "pw",
                    new RegisterRequest("홍길동", "testuser1", "Te1!", "test@example.com", "길동이")),
                Arguments.of("이메일 형식이 올바르지 않으면 400을 반환한다", "email",
                    new RegisterRequest("홍길동", "testuser1", "Test1234!", "invalid-email", "길동이")),
                Arguments.of("닉네임이 빈 값이면 400을 반환한다", "nickname",
                    new RegisterRequest("홍길동", "testuser1", "Test1234!", "test@example.com", ""))
            );
        }
        
        @Test
        @DisplayName("필수 필드가 모두 누락되면 400과 모든 검증 에러를 반환한다")
        void register_allFieldsMissing_returns400() throws Exception {
            RegisterRequest request = createRequest("", "", "", "", "");
            
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validation.name").exists())
                .andExpect(jsonPath("$.validation.id").exists())
                .andExpect(jsonPath("$.validation.pw").exists())
                .andExpect(jsonPath("$.validation.email").exists())
                .andExpect(jsonPath("$.validation.nickname").exists())
                .andDo(print());
        }
        
        @Test
        @DisplayName("요청 바디가 없으면 400을 반환한다")
        void register_missingBody_returns400() throws Exception {
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8"))
                .andExpect(status().isBadRequest())
                .andDo(print());
        }
    }
    
    @Nested
    @DisplayName("회원가입 실패 - 비즈니스 로직")
    class RegisterBusinessFail {
        
        @ParameterizedTest(name = "{0}")
        @MethodSource("registerBusinessExceptions")
        void register_businessException_returnsExpectedStatus(String description, ErrorCode errorCode, int expectedStatus) throws Exception {
            RegisterRequest request = createRegisterRequest();
            given(registerService.register(any(RegisterRequest.class)))
                .willThrow(new CustomException(errorCode));
            
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.error").value(errorCode.name()))
                .andDo(print());
        }
        
        static Stream<Arguments> registerBusinessExceptions() {
            return Stream.of(
                Arguments.of("이미 존재하는 아이디면 409를 반환한다", ErrorCode.DUPLICATE_ID, 409),
                Arguments.of("이미 존재하는 사용자명이면 409를 반환한다", ErrorCode.DUPLICATE_USERNAME, 409),
                Arguments.of("이메일 인증이 완료되지 않았으면 400을 반환한다", ErrorCode.EMAIL_NOT_VERIFIED, 400),
                Arguments.of("이메일 인증 코드를 찾을 수 없으면 404를 반환한다", ErrorCode.EMAIL_CODE_NOT_FOUND, 404)
            );
        }
    }
}