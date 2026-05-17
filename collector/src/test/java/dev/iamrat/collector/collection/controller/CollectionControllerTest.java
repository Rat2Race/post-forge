package dev.iamrat.collector.collection.controller;

import dev.iamrat.collector.collection.service.DataSourceCollector;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CollectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CollectionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean(name = "naverNewsCollector")
    DataSourceCollector naverNewsCollector;

    @MockitoBean(name = "rssCollector")
    DataSourceCollector rssCollector;

    @BeforeEach
    void setUpCollectors() {
        given(naverNewsCollector.getSourceName()).willReturn(NewsDocumentMetadata.SOURCE_NAVER_NEWS);
        given(rssCollector.getSourceName()).willReturn("rss");
    }

    @Test
    @DisplayName("source가 일치하는 collector만 실행한다")
    void trigger_matchingSource_runsOnlyTargetCollector() throws Exception {
        mockMvc.perform(post("/collector/" + NewsDocumentMetadata.SOURCE_NAVER_NEWS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(NewsDocumentMetadata.SOURCE_NAVER_NEWS + " 수집 완료"));

        verify(naverNewsCollector).collect();
        verify(rssCollector, never()).collect();
    }

    @Test
    @DisplayName("알 수 없는 source는 사용 가능한 collector 목록과 함께 400을 반환한다")
    void trigger_unknownSource_returnsAvailableCollectors() throws Exception {
        mockMvc.perform(post("/collector/unknown"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Unknown source"))
            .andExpect(jsonPath("$.available").value(allOf(
                containsString(NewsDocumentMetadata.SOURCE_NAVER_NEWS),
                containsString("rss")
            )));

        verify(naverNewsCollector, never()).collect();
        verify(rssCollector, never()).collect();
    }
}
