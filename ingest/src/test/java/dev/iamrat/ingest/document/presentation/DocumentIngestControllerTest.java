package dev.iamrat.ingest.document.presentation;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ingest.document.application.DocumentIngestResult;
import dev.iamrat.ingest.document.application.IngestDocumentsUseCase;
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

@WebMvcTest(DocumentIngestController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentIngestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    IngestDocumentsUseCase ingestDocumentsUseCase;

    @Test
    @DisplayName("문서 적재 표준 엔드포인트는 ingest 경로를 사용한다")
    void ingest_storesDocuments() throws Exception {
        List<DocumentIngestRequest> requests = requests();
        given(ingestDocumentsUseCase.ingest(anyList()))
            .willReturn(new DocumentIngestResult(1, 3));

        mockMvc.perform(post("/api/ingest/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.documentCount").value(1))
            .andExpect(jsonPath("$.chunkCount").value(3))
            .andExpect(jsonPath("$.count").doesNotExist())
            .andExpect(jsonPath("$.embeddingsStored").doesNotExist())
            .andExpect(jsonPath("$.degradationReason").doesNotExist())
            .andExpect(jsonPath("$.message").doesNotExist());

        verify(ingestDocumentsUseCase).ingest(anyList());
    }

    @Test
    @DisplayName("레거시 AI 문서 적재 경로는 더 이상 매핑하지 않는다")
    void store_legacyAiPath_returnsNotFound() throws Exception {
        List<DocumentIngestRequest> requests = requests();

        mockMvc.perform(post("/ai/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isNotFound());

        verifyNoInteractions(ingestDocumentsUseCase);
    }

    private List<DocumentIngestRequest> requests() {
        return List.of(new DocumentIngestRequest(
            "content",
            "manual",
            Map.of("keyword", "tech")
        ));
    }
}
