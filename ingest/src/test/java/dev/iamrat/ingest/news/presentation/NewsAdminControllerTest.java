package dev.iamrat.ingest.news.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.ingest.document.application.DocumentIngestResult;
import dev.iamrat.ingest.news.application.IngestProductNewsUseCase;
import dev.iamrat.ingest.news.application.LaunchNewsPublishCommand;
import dev.iamrat.ingest.news.application.LaunchNewsPublishResult;
import dev.iamrat.ingest.news.application.LaunchNewsSkip;
import dev.iamrat.ingest.news.application.LaunchNewsSkipReason;
import dev.iamrat.ingest.news.application.ProductNewsIngestResult;
import dev.iamrat.ingest.news.application.PublishLaunchNewsUseCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NewsAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class NewsAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IngestProductNewsUseCase ingestProductNewsUseCase;

    @MockitoBean
    private PublishLaunchNewsUseCase publishLaunchNewsUseCase;

    @Test
    @DisplayName("관리자 출시 뉴스 수동 게시 엔드포인트는 backfill origin으로 서비스를 호출한다")
    void publishLaunchNewsManual_callsUseCaseWithBackfillOrigin() throws Exception {
        given(publishLaunchNewsUseCase.publish(any())).willReturn(new LaunchNewsPublishResult(
            "갤럭시북",
            List.of(42L),
            List.of(new LaunchNewsSkip("https://news.example/duplicate", LaunchNewsSkipReason.DUPLICATE_ARTICLE))
        ));

        mockMvc.perform(post("/api/admin/launch-news/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LaunchNewsPublishRequest(
                    "갤럭시북",
                    10L,
                    5,
                    3,
                    List.of("출시")
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keyword").value("갤럭시북"))
            .andExpect(jsonPath("$.publishedCount").value(1))
            .andExpect(jsonPath("$.skippedCount").value(1))
            .andExpect(jsonPath("$.createdPostIds[0]").value(42L))
            .andExpect(jsonPath("$.skips[0].reason").value("DUPLICATE_ARTICLE"));

        ArgumentCaptor<LaunchNewsPublishCommand> captor = ArgumentCaptor.forClass(LaunchNewsPublishCommand.class);
        verify(publishLaunchNewsUseCase).publish(captor.capture());
        assertThat(captor.getValue().publishOrigin()).isEqualTo(PostPublishOrigin.ADMIN_BACKFILL);
    }

    @Test
    @DisplayName("관리자 뉴스 문서 수집 엔드포인트는 완료 결과를 반환한다")
    void ingestProductNewsManual_returnsCompletedResult() throws Exception {
        given(ingestProductNewsUseCase.ingest(any(), any(), any(), any()))
            .willReturn(new ProductNewsIngestResult(
                "갤럭시북",
                10L,
                List.of("갤럭시북 출시"),
                new DocumentIngestResult(1, 2)
            ));

        mockMvc.perform(post("/api/admin/news-documents/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProductNewsIngestRequest(
                    10L,
                    "갤럭시북",
                    5,
                    List.of("출시")
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.newsCount").value(1))
            .andExpect(jsonPath("$.chunkCount").value(2))
            .andExpect(jsonPath("$.embeddingsStored").doesNotExist())
            .andExpect(jsonPath("$.degradationReason").doesNotExist());
    }

    @Test
    @DisplayName("수동 출시 뉴스 게시 요청의 키워드가 비어 있으면 검증 오류를 반환한다")
    void publishLaunchNewsManual_blankKeywordReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/admin/launch-news/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LaunchNewsPublishRequest(
                    "",
                    null,
                    5,
                    3,
                    List.of("출시")
                ))))
            .andExpect(status().isBadRequest());
    }
}
