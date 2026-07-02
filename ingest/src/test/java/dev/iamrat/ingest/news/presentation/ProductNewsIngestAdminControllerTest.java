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
import dev.iamrat.ingest.news.application.CollectProductNewsDocumentsUseCase;
import dev.iamrat.ingest.news.application.LaunchNewsAutoPostRequest;
import dev.iamrat.ingest.news.application.LaunchNewsAutoPostResult;
import dev.iamrat.ingest.news.application.LaunchNewsAutoPostService;
import dev.iamrat.ingest.news.application.LaunchNewsSkip;
import dev.iamrat.ingest.news.application.LaunchNewsSkipReason;
import dev.iamrat.ingest.news.presentation.dto.LaunchNewsManualPostRequest;
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

@WebMvcTest(ProductNewsIngestAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductNewsIngestAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CollectProductNewsDocumentsUseCase collectProductNewsDocumentsUseCase;

    @MockitoBean
    private LaunchNewsAutoPostService launchNewsAutoPostService;

    @Test
    @DisplayName("관리자 출시 뉴스 수동 게시 엔드포인트는 backfill origin으로 서비스를 호출한다")
    void postLaunchNewsManual_callsServiceWithBackfillOrigin() throws Exception {
        given(launchNewsAutoPostService.postLaunchNews(any())).willReturn(new LaunchNewsAutoPostResult(
            "갤럭시북",
            1,
            1,
            List.of(42L),
            List.of(new LaunchNewsSkip("https://news.example/duplicate", LaunchNewsSkipReason.DUPLICATE_ARTICLE))
        ));

        mockMvc.perform(post("/api/admin/launch-news/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LaunchNewsManualPostRequest(
                    "갤럭시북",
                    10L,
                    5,
                    3,
                    List.of("출시")
                ))))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.keyword").value("갤럭시북"))
            .andExpect(jsonPath("$.acceptedCount").value(1))
            .andExpect(jsonPath("$.skippedCount").value(1))
            .andExpect(jsonPath("$.createdPostIds[0]").value(42L))
            .andExpect(jsonPath("$.skips[0].reason").value("DUPLICATE_ARTICLE"));

        ArgumentCaptor<LaunchNewsAutoPostRequest> captor = ArgumentCaptor.forClass(LaunchNewsAutoPostRequest.class);
        verify(launchNewsAutoPostService).postLaunchNews(captor.capture());
        assertThat(captor.getValue().publishOrigin()).isEqualTo(PostPublishOrigin.ADMIN_BACKFILL);
    }

    @Test
    @DisplayName("수동 출시 뉴스 게시 요청의 키워드가 비어 있으면 검증 오류를 반환한다")
    void postLaunchNewsManual_blankKeywordReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/admin/launch-news/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LaunchNewsManualPostRequest(
                    "",
                    null,
                    5,
                    3,
                    List.of("출시")
                ))))
            .andExpect(status().isBadRequest());
    }
}
