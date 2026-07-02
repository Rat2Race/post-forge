package dev.iamrat.ingest.pipeline.presentation;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ingest.pipeline.application.DocumentIngestResult;
import dev.iamrat.ingest.pipeline.application.IngestPipelineService;
import dev.iamrat.ingest.pipeline.presentation.dto.DocumentRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    IngestPipelineService ingestPipelineService;

    @Test
    @DisplayName("문서 적재 표준 엔드포인트는 ingest 경로를 사용한다")
    void store_ingestPath_storesDocuments() throws Exception {
        List<DocumentRequest> requests = requests();
        given(ingestPipelineService.store(anyList()))
            .willReturn(new DocumentIngestResult(1, 1, true, null));

        mockMvc.perform(post("/api/ingest/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.chunkCount").value(1))
            .andExpect(jsonPath("$.embeddingsStored").value(true))
            .andExpect(jsonPath("$.degradationReason").doesNotExist())
            .andExpect(jsonPath("$.message").value("문서가 저장되었습니다."));

        verify(ingestPipelineService).store(anyList());
    }

    @Test
    @DisplayName("임베딩 저장이 불가하면 성능 저하 상태를 응답에 표시한다")
    void store_whenEmbeddingsUnavailable_returnsDegradedStatus() throws Exception {
        List<DocumentRequest> requests = requests();
        given(ingestPipelineService.store(anyList()))
            .willReturn(new DocumentIngestResult(1, 1, false, "VECTOR_STORE_UNAVAILABLE"));

        mockMvc.perform(post("/api/ingest/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.chunkCount").value(1))
            .andExpect(jsonPath("$.embeddingsStored").value(false))
            .andExpect(jsonPath("$.degradationReason").value("VECTOR_STORE_UNAVAILABLE"))
            .andExpect(jsonPath("$.message").value("문서가 임베딩 없이 접수되었습니다."));
    }

    @Test
    @DisplayName("레거시 AI 문서 적재 경로는 더 이상 매핑하지 않는다")
    void store_legacyAiPath_returnsNotFound() throws Exception {
        List<DocumentRequest> requests = requests();

        mockMvc.perform(post("/ai/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isNotFound());

        verifyNoInteractions(ingestPipelineService);
    }

    private List<DocumentRequest> requests() {
        return List.of(new DocumentRequest(
            "content",
            "manual",
            Map.of("keyword", "tech")
        ));
    }
}
