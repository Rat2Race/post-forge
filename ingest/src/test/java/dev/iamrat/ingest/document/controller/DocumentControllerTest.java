package dev.iamrat.ingest.document.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ingest.document.dto.DocumentRequest;
import dev.iamrat.ingest.document.service.DocumentService;
import dev.iamrat.support.web.GlobalExceptionHandler;
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

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DocumentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    DocumentService documentService;

    @Test
    @DisplayName("문서 적재 canonical endpoint는 ingest 경로를 사용한다")
    void store_ingestPath_storesDocuments() throws Exception {
        List<DocumentRequest> requests = requests();

        mockMvc.perform(post("/ingest/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.message").value("문서가 저장되었습니다."));

        verify(documentService).store(anyList());
    }

    @Test
    @DisplayName("기존 ai 문서 적재 경로도 호환용으로 유지한다")
    void store_legacyAiPath_storesDocuments() throws Exception {
        List<DocumentRequest> requests = requests();

        mockMvc.perform(post("/ai/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1));

        verify(documentService).store(anyList());
    }

    private List<DocumentRequest> requests() {
        return List.of(new DocumentRequest(
            "content",
            "manual",
            Map.of("keyword", "tech")
        ));
    }
}
