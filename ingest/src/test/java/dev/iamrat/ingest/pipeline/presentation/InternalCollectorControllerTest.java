package dev.iamrat.ingest.pipeline.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.ingest.pipeline.application.SourceDocumentIngestService;
import dev.iamrat.ingest.pipeline.presentation.dto.DocumentRequest;
import dev.iamrat.ingest.support.web.TestExceptionResponseHandler;
import java.util.List;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalCollectorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestExceptionResponseHandler.class)
class InternalCollectorControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    SourceDocumentIngestService sourceDocumentIngestService;

    @Test
    @DisplayName("collector 문서를 저장한 뒤 뉴스 자동 게시 오케스트레이터를 호출한다")
    void ingestDocuments_storesDocumentsAndTriggersAutoPost() throws Exception {
        List<DocumentRequest> requests = List.of(new DocumentRequest(
            "content",
            NewsDocumentMetadata.SOURCE_NAVER_NEWS,
            NewsDocumentMetadata.autoPostEligible(
                "테크",
                "AI 반도체 수요 증가",
                "https://news.example/1",
                ""
            ).toMap()
        ));

        mockMvc.perform(post("/internal/collector/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.message").value("문서가 저장되었습니다."));

        verify(sourceDocumentIngestService).ingestCommands(anyList());
    }
}
