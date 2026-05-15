package dev.iamrat.ingest.internal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ingest.document.dto.DocumentRequest;
import dev.iamrat.ingest.document.service.DocumentService;
import dev.iamrat.support.web.GlobalExceptionHandler;
import dev.iamrat.ingest.internal.service.AutoPostOrchestrator;
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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
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
    AutoPostOrchestrator autoPostOrchestrator;

    @Test
    @DisplayName("crawl 문서를 저장한 뒤 뉴스 자동 게시 오케스트레이터를 호출한다")
    void ingestDocuments_storesDocumentsAndTriggersAutoPost() throws Exception {
        List<DocumentRequest> requests = List.of(new DocumentRequest(
            "content",
            "naver-news",
            Map.of("keyword", "테크", "newsTitle", "AI 반도체 수요 증가", "originalLink", "https://news.example/1")
        ));
        given(autoPostOrchestrator.publishEligible(anyList())).willReturn(1);

        mockMvc.perform(post("/internal/crawl/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.message").value("문서가 저장되었습니다."));

        var inOrder = inOrder(documentService, autoPostOrchestrator);
        inOrder.verify(documentService).store(anyList());
        inOrder.verify(autoPostOrchestrator).publishEligible(anyList());
    }
}
