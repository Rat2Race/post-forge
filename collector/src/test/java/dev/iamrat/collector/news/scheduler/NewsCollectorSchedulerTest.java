package dev.iamrat.collector.news.scheduler;

import dev.iamrat.collector.news.service.NaverNewsCollectorService;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NewsCollectorSchedulerTest {

    @Mock
    NaverNewsCollectorService naverNewsCollectorService;

    @InjectMocks
    NewsCollectorScheduler newsCollectorScheduler;

    @Test
    @DisplayName("스케줄 실행 시 네이버 뉴스 collector를 호출한다")
    void collectNaverNews_delegatesToNaverNewsCollector() {
        given(naverNewsCollectorService.getSourceName()).willReturn(NewsDocumentMetadata.SOURCE_NAVER_NEWS);

        newsCollectorScheduler.collectNaverNews();

        verify(naverNewsCollectorService).collect();
    }

    @Test
    @DisplayName("collector 예외는 스케줄러 밖으로 전파하지 않는다")
    void collectNaverNews_collectorFails_doesNotPropagate() {
        given(naverNewsCollectorService.getSourceName()).willReturn(NewsDocumentMetadata.SOURCE_NAVER_NEWS);
        willThrow(new RuntimeException("collector failed"))
            .given(naverNewsCollectorService)
            .collect();

        assertThatNoException().isThrownBy(newsCollectorScheduler::collectNaverNews);

        verify(naverNewsCollectorService).collect();
    }
}
