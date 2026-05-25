package dev.iamrat.auth.register.presentation;

import dev.iamrat.auth.support.error.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.core.global.error.ErrorCode;
import dev.iamrat.auth.support.web.TestExceptionResponseHandler;
import dev.iamrat.auth.register.application.RegisterCommand;
import dev.iamrat.auth.register.presentation.dto.RegisterRequest;
import dev.iamrat.auth.register.application.RegisterService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("webmvc")
@WebMvcTest(controllers = RegisterController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestExceptionResponseHandler.class)
class RegisterControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    RegisterService registerService;

    private RegisterRequest createRegisterRequest() {
        return new RegisterRequest("testuser1", "Test1234!", "test@example.com", "길동이");
    }

    @Nested
    @DisplayName("회원가입 성공")
    class RegisterSuccess {

        @Test
        @DisplayName("유효한 요청이면 201 Created를 반환한다")
        void register_validRequest_returns201() throws Exception {
            RegisterRequest request = createRegisterRequest();
            given(registerService.register(any(RegisterCommand.class))).willReturn(1L);

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1L))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
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
                .andExpect(jsonPath("$.validation." + fieldName).exists());
        }

        static Stream<Arguments> invalidRegisterRequests() {
            return Stream.of(
                Arguments.of("username이 4자 미만이면 400을 반환한다", "username",
                    new RegisterRequest("abc", "Test1234!", "test@example.com", "길동이")),
                Arguments.of("비밀번호에 대문자/특수문자가 없으면 400을 반환한다", "password",
                    new RegisterRequest("testuser1", "test1234", "test@example.com", "길동이")),
                Arguments.of("비밀번호가 8자 미만이면 400을 반환한다", "password",
                    new RegisterRequest("testuser1", "Te1!", "test@example.com", "길동이")),
                Arguments.of("이메일 형식이 올바르지 않으면 400을 반환한다", "email",
                    new RegisterRequest("testuser1", "Test1234!", "invalid-email", "길동이")),
                Arguments.of("닉네임이 빈 값이면 400을 반환한다", "nickname",
                    new RegisterRequest("testuser1", "Test1234!", "test@example.com", ""))
            );
        }

        @Test
        @DisplayName("요청 바디가 없으면 400을 반환한다")
        void register_missingBody_returns400() throws Exception {
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("회원가입 실패 - 비즈니스 로직")
    class RegisterBusinessFail {

        @ParameterizedTest(name = "{0}")
        @MethodSource("registerBusinessExceptions")
        void register_businessException_returnsExpectedStatus(String description, ErrorCode errorCode, int expectedStatus) throws Exception {
            RegisterRequest request = createRegisterRequest();
            given(registerService.register(any(RegisterCommand.class)))
                .willThrow(new CustomException(errorCode));

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.error").value(errorCode.name()));
        }

        static Stream<Arguments> registerBusinessExceptions() {
            return Stream.of(
                Arguments.of("이미 존재하는 username이면 409를 반환한다", AuthErrorCode.DUPLICATE_USERNAME, 409),
                Arguments.of("이메일 인증이 완료되지 않았으면 400을 반환한다", AuthErrorCode.EMAIL_NOT_VERIFIED, 400),
                Arguments.of("이메일 인증 코드를 찾을 수 없으면 404를 반환한다", AuthErrorCode.EMAIL_CODE_NOT_FOUND, 404)
            );
        }
    }
}
