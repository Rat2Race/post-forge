package dev.iamrat.pipeline.service;

import dev.iamrat.candidate.entity.CandidateSelection;
import dev.iamrat.common.InternalCrawlClient;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CandidatePostPublisherTest {

    @Mock
    private InternalCrawlClient internalCrawlClient;

    @InjectMocks
    private CandidatePostPublisher candidatePostPublisher;

    @Test
    @DisplayName("성공한 게시글 생성 요청만 집계한다")
    void publish_countsOnlySuccessfulRequests() {
        List<CandidateSelection> selections = List.of(
            selection("005930", "삼성전자"),
            selection("000660", "SK하이닉스"),
            selection("035420", "NAVER")
        );
        given(internalCrawlClient.requestPostGeneration("005930", "삼성전자")).willReturn(true);
        given(internalCrawlClient.requestPostGeneration("000660", "SK하이닉스")).willReturn(false);
        given(internalCrawlClient.requestPostGeneration("035420", "NAVER")).willReturn(true);

        int published = candidatePostPublisher.publish(selections, 10);

        assertThat(published).isEqualTo(2);
        verify(internalCrawlClient).requestPostGeneration("005930", "삼성전자");
        verify(internalCrawlClient).requestPostGeneration("000660", "SK하이닉스");
        verify(internalCrawlClient).requestPostGeneration("035420", "NAVER");
    }

    @Test
    @DisplayName("음수 limit은 0으로 처리한다")
    void publish_negativeLimitPublishesNothing() {
        int published = candidatePostPublisher.publish(List.of(selection("005930", "삼성전자")), -1);

        assertThat(published).isZero();
        verify(internalCrawlClient, times(0)).requestPostGeneration("005930", "삼성전자");
    }

    private CandidateSelection selection(String ticker, String stockName) {
        return new CandidateSelection(
            null,
            LocalDate.of(2026, 4, 10),
            ticker,
            stockName,
            "reason",
            null,
            null,
            0L,
            0L,
            false,
            0,
            0,
            0,
            null
        );
    }
}

