package dev.iamrat.auth.email.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.auth.email.dto.SendEmailRequest;
import dev.iamrat.auth.email.service.EmailVerificationService;
import dev.iamrat.support.web.GlobalExceptionHandler;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("webmvc")
@WebMvcTest(EmailVerificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmailVerificationControllerTest {
    
    @Autowired
    MockMvc mockMvc;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @MockitoBean
    EmailVerificationService emailVerificationService;
    
    @Nested
    @DisplayName("인증 메일 발송 테스트")
    class sendVerificationEmailTest {
        
        @Test
        @DisplayName("정상적인 이메일이 오면 200 + 성공 메시지를 응답한다")
        void send_validEmail_returns200() throws Exception {
            SendEmailRequest request = new SendEmailRequest("tester@test.com");
            
            mockMvc.perform(post("/auth/email/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 메일이 발송되었습니다."));
        }
        
        @Test
        @DisplayName("이메일이 빈 값이면 400 예외가 발생한다")
        void send_blankEmail_returns400() throws Exception {
            SendEmailRequest request = new SendEmailRequest("");
            
            mockMvc.perform(post("/auth/email/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
        
    }
    
    @Nested
    @DisplayName("토큰 검증 테스트")
    class verifyEmailTest {
        
        @Test
        @DisplayName("정상적인 토큰이 오면 200 + 성공 메시지를 응답한다")
        void verify_validToken_returns200() throws Exception {
            String token = "valid-token";
            
            given(emailVerificationService.verifyEmail(token))
                .willReturn("valid-email"   );
            
            mockMvc.perform(get("/auth/email/verify")
                    .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."))
                .andExpect(jsonPath("$.email").value("valid-email"));
        }
        
        @Test
        @DisplayName("토큰 파라미터가 없으면 400 예외가 발생한다")
        void verify_missingToken_returnsError() throws Exception {
            mockMvc.perform(get("/auth/email/verify"))
                .andExpect(status().isBadRequest());
        }
    }
}
