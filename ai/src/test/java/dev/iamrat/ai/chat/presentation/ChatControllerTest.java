package dev.iamrat.ai.chat.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.chat.application.ChatService;
import dev.iamrat.core.account.UserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ChatService chatService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("채팅 요청 성공")
    class ChatSuccess {

        @Test
        @DisplayName("유효한 메시지로 채팅하면 200과 응답을 반환한다")
        void chat_validMessage_returns200() throws Exception {
            ChatRequest request = new ChatRequest("오늘 테크 트렌드 요약해줘");
            UserPrincipal principal = () -> 42L;
            SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of())
            );
            given(chatService.chat(anyString(), eq(42L), eq("203.0.113.10")))
                .willReturn("오늘의 테크 트렌드 요약입니다.");

            mockMvc.perform(post("/api/ai/chat")
                    .with(servletRequest -> {
                        servletRequest.setRemoteAddr("203.0.113.10");
                        return servletRequest;
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("오늘의 테크 트렌드 요약입니다."));

            verify(chatService).chat("오늘 테크 트렌드 요약해줘", 42L, "203.0.113.10");
        }
    }

    @Nested
    @DisplayName("채팅 요청 실패")
    class ChatFail {

        @Test
        @DisplayName("메시지가 빈 값이면 400을 반환한다")
        void chat_emptyMessage_returns400() throws Exception {
            ChatRequest request = new ChatRequest("");

            mockMvc.perform(post("/api/ai/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("요청 바디가 없으면 400을 반환한다")
        void chat_missingBody_returns400() throws Exception {
            mockMvc.perform(post("/api/ai/chat")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }
    }
}
