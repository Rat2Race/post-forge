package dev.iamrat.ingest.news.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.ingest.news.application.IngestProductNewsUseCase;
import dev.iamrat.ingest.news.application.LaunchNewsPublishCommand;
import dev.iamrat.ingest.news.application.LaunchNewsPublishResult;
import dev.iamrat.ingest.news.application.LaunchNewsSkip;
import dev.iamrat.ingest.news.application.LaunchNewsSkipReason;
import dev.iamrat.ingest.news.application.DailyDigestPublishResult;
import dev.iamrat.ingest.news.application.DailyDigestSkipReason;
import dev.iamrat.ingest.news.application.ProductNewsIngestResult;
import dev.iamrat.ingest.news.application.PublishDailyDigestUseCase;
import dev.iamrat.ingest.news.application.PublishLaunchNewsUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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

    @MockitoBean
    private PublishDailyDigestUseCase publishDailyDigestUseCase;

    @MockitoBean
    private Clock clock;

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
                    5,
                    3,
                    List.of("출시"),
                    null
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
    @DisplayName("관리자 출시 뉴스 수동 게시 요청의 분야 카테고리가 커맨드에 실린다")
    void publishLaunchNewsManual_carriesCategoryIntoCommand() throws Exception {
        given(publishLaunchNewsUseCase.publish(any())).willReturn(new LaunchNewsPublishResult(
            "갤럭시북",
            List.of(42L),
            List.of()
        ));

        mockMvc.perform(post("/api/admin/launch-news/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LaunchNewsPublishRequest(
                    "갤럭시북",
                    5,
                    3,
                    List.of("출시"),
                    BoardCategory.DIGITAL
                ))))
            .andExpect(status().isOk());

        ArgumentCaptor<LaunchNewsPublishCommand> captor = ArgumentCaptor.forClass(LaunchNewsPublishCommand.class);
        verify(publishLaunchNewsUseCase).publish(captor.capture());
        assertThat(captor.getValue().category()).isEqualTo(BoardCategory.DIGITAL);
    }

    @Test
    @DisplayName("관리자 뉴스 문서 수집 엔드포인트는 완료 결과를 반환한다")
    void ingestProductNewsManual_returnsCompletedResult() throws Exception {
        given(ingestProductNewsUseCase.ingest(any(), any(), any()))
            .willReturn(new ProductNewsIngestResult(
                "갤럭시북",
                List.of("갤럭시북 출시"),
                1,
                2
            ));

        mockMvc.perform(post("/api/admin/news-documents/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProductNewsIngestRequest(
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
    @DisplayName("문서 수집 요청의 displayCount가 허용 범위를 넘으면 조용히 깎지 않고 거절한다")
    void ingestProductNewsManual_rejectsDisplayCountAboveAllowedRange() throws Exception {
        mockMvc.perform(post("/api/admin/news-documents/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProductNewsIngestRequest(
                    "갤럭시북",
                    101,
                    List.of("출시")
                ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("문서 수집 요청의 displayCount가 1 미만이면 거절한다")
    void ingestProductNewsManual_rejectsDisplayCountBelowAllowedRange() throws Exception {
        mockMvc.perform(post("/api/admin/news-documents/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProductNewsIngestRequest(
                    "갤럭시북",
                    0,
                    List.of("출시")
                ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("관리자 데일리 브리핑 수동 게시 요청의 날짜가 유스케이스에 도달한다")
    void publishDailyDigestManual_reachesUseCaseWithRequestedDate() throws Exception {
        given(publishDailyDigestUseCase.publish(LocalDate.of(2026, 8, 19))).willReturn(new DailyDigestPublishResult(
            LocalDate.of(2026, 8, 19),
            List.of(42L),
            Map.of(BoardCategory.SPORTS, DailyDigestSkipReason.NO_SOURCE)
        ));

        mockMvc.perform(post("/api/admin/news/digest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DailyDigestPublishRequest(LocalDate.of(2026, 8, 19)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.newsDate").value("2026-08-19"))
            .andExpect(jsonPath("$.publishedCount").value(1))
            .andExpect(jsonPath("$.skippedCount").value(1))
            .andExpect(jsonPath("$.createdPostIds[0]").value(42L))
            .andExpect(jsonPath("$.skips.SPORTS").value("NO_SOURCE"));

        verify(publishDailyDigestUseCase).publish(LocalDate.of(2026, 8, 19));
    }

    @Test
    @DisplayName("관리자 데일리 브리핑 수동 게시는 날짜가 없으면 어제로 게시한다")
    void publishDailyDigestManual_defaultsToYesterday() throws Exception {
        given(clock.instant()).willReturn(Instant.parse("2026-08-21T06:00:00Z"));
        given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
        given(publishDailyDigestUseCase.publish(LocalDate.of(2026, 8, 20))).willReturn(new DailyDigestPublishResult(
            LocalDate.of(2026, 8, 20),
            List.of(),
            Map.of()
        ));

        mockMvc.perform(post("/api/admin/news/digest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DailyDigestPublishRequest(null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.newsDate").value("2026-08-20"));

        verify(publishDailyDigestUseCase).publish(LocalDate.of(2026, 8, 20));
    }

    @Test
    @DisplayName("수동 출시 뉴스 게시 요청의 키워드가 비어 있으면 검증 오류를 반환한다")
    void publishLaunchNewsManual_blankKeywordReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/admin/launch-news/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LaunchNewsPublishRequest(
                    "",
                    5,
                    3,
                    List.of("출시"),
                    null
                ))))
            .andExpect(status().isBadRequest());
    }
}
