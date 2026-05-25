package dev.iamrat.collector.source.presentation;

import dev.iamrat.collector.source.application.SourceCollectionResult;
import dev.iamrat.collector.source.application.SourceCollectionService;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SourceCollectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SourceCollectionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SourceCollectionService sourceCollectionService;

    @Test
    @DisplayName("source가 일치하는 collector만 실행한다")
    void trigger_matchingSource_runsOnlyTargetCollector() throws Exception {
        given(sourceCollectionService.trigger(NewsDocumentMetadata.SOURCE_NAVER_NEWS))
            .willReturn(SourceCollectionResult.collected(
                NewsDocumentMetadata.SOURCE_NAVER_NEWS,
                List.of(NewsDocumentMetadata.SOURCE_NAVER_NEWS, "rss")
            ));

        mockMvc.perform(post("/collector/" + NewsDocumentMetadata.SOURCE_NAVER_NEWS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(NewsDocumentMetadata.SOURCE_NAVER_NEWS + " 수집 완료"));
    }

    @Test
    @DisplayName("알 수 없는 source는 사용 가능한 collector 목록과 함께 400을 반환한다")
    void trigger_unknownSource_returnsAvailableCollectors() throws Exception {
        given(sourceCollectionService.trigger("unknown"))
            .willReturn(SourceCollectionResult.unknown(
                "unknown",
                List.of(NewsDocumentMetadata.SOURCE_NAVER_NEWS, "rss")
            ));

        mockMvc.perform(post("/collector/unknown"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Unknown source"))
            .andExpect(jsonPath("$.available").value(allOf(
                containsString(NewsDocumentMetadata.SOURCE_NAVER_NEWS),
                containsString("rss")
            )));
    }
}
