package dev.iamrat.internal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.post.dto.GeneratedPost;
import dev.iamrat.ai.post.dto.PostGenerationRequest;
import dev.iamrat.ai.post.service.PostGenerationService;
import dev.iamrat.global.exception.GlobalExceptionHandler;
import dev.iamrat.document.dto.DocumentRequest;
import dev.iamrat.document.service.DocumentService;
import java.util.List;
import java.util.Map;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalCrawlController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class InternalCrawlControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    DocumentService documentService;

    @MockitoBean
    PostGenerationService postGenerationService;

    @Test
    @DisplayName("crawl 문서를 받아 vector 저장 서비스로 넘긴다")
    void ingestDocuments_storesDocuments() throws Exception {
        List<DocumentRequest> requests = List.of(new DocumentRequest(
                "content",
                "naver-news",
                Map.of("keyword", "주식")
        ));

        mockMvc.perform(post("/internal/crawl/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(documentService).store(anyList());
    }

    @Test
    @DisplayName("crawl 후보 요청을 받아 게시글을 생성하고 발행한다")
    void generatePost_publishesImmediately() throws Exception {
        GeneratedPost post = new GeneratedPost("제목", "요약", "내용", List.of("태그"));
        given(postGenerationService.generate(anyString(), anyString())).willReturn(post);
        given(postGenerationService.publish(any(GeneratedPost.class))).willReturn(1L);

        mockMvc.perform(post("/internal/crawl/posts/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PostGenerationRequest("005930", "삼성전자"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(1L));
    }
}
