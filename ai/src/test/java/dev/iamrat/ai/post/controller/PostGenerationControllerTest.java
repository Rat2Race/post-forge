package dev.iamrat.ai.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.post.dto.GeneratedPost;
import dev.iamrat.ai.post.dto.PostGenerationRequest;
import dev.iamrat.ai.post.service.PostGenerationService;
import dev.iamrat.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostGenerationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PostGenerationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PostGenerationService postGenerationService;

    @Test
    @DisplayName("생성 요청이면 게시글을 바로 발행한다")
    void generate_request_publishesImmediately() throws Exception {
        GeneratedPost post = new GeneratedPost("제목", "요약", "내용", java.util.List.of("태그"));
        given(postGenerationService.generate(anyString(), anyString())).willReturn(post);
        given(postGenerationService.publish(any(GeneratedPost.class))).willReturn(1L);

        mockMvc.perform(post("/ai/posts/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PostGenerationRequest("005930", "삼성전자"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.postId").value(1L));
    }

    @Test
    @DisplayName("요청 바디가 유효하지 않으면 400을 반환한다")
    void generate_invalidRequest_returns400() throws Exception {
        GeneratedPost post = new GeneratedPost("제목", "요약", "내용", java.util.List.of("태그"));
        given(postGenerationService.generate(anyString(), anyString())).willReturn(post);

        mockMvc.perform(post("/ai/posts/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PostGenerationRequest("", "삼성전자"))))
            .andExpect(status().isBadRequest());
    }
}
