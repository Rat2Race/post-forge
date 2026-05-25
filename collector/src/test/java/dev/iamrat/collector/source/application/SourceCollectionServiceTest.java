package dev.iamrat.collector.source.application;

import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SourceCollectionServiceTest {

    @Mock
    private DataSourceCollector naverNewsCollector;

    @Mock
    private DataSourceCollector rssCollector;

    private SourceCollectionService sourceCollectionService;

    @BeforeEach
    void setUp() {
        given(naverNewsCollector.getSourceName()).willReturn(NewsDocumentMetadata.SOURCE_NAVER_NEWS);
        given(rssCollector.getSourceName()).willReturn("rss");
        sourceCollectionService = new SourceCollectionService(List.of(naverNewsCollector, rssCollector));
    }

    @Test
    @DisplayName("source가 일치하는 collector만 실행한다")
    void trigger_matchingSource_runsOnlyTargetCollector() {
        SourceCollectionResult result = sourceCollectionService.trigger(NewsDocumentMetadata.SOURCE_NAVER_NEWS);

        assertThat(result.collected()).isTrue();
        assertThat(result.source()).isEqualTo(NewsDocumentMetadata.SOURCE_NAVER_NEWS);
        assertThat(result.availableSources()).containsExactly(NewsDocumentMetadata.SOURCE_NAVER_NEWS, "rss");
        verify(naverNewsCollector).collect();
        verify(rssCollector, never()).collect();
    }

    @Test
    @DisplayName("알 수 없는 source는 available source 목록과 함께 실패 결과를 반환한다")
    void trigger_unknownSource_returnsAvailableSources() {
        SourceCollectionResult result = sourceCollectionService.trigger("unknown");

        assertThat(result.collected()).isFalse();
        assertThat(result.source()).isEqualTo("unknown");
        assertThat(result.availableSources()).containsExactly(NewsDocumentMetadata.SOURCE_NAVER_NEWS, "rss");
        verify(naverNewsCollector, never()).collect();
        verify(rssCollector, never()).collect();
    }
}
