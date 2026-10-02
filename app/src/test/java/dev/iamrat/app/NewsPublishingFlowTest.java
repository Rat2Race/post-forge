package dev.iamrat.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.iamrat.ai.search.application.SearchPort;
import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.core.board.post.DailyDigestSourceItem;
import dev.iamrat.core.board.post.LaunchNewsPost;
import dev.iamrat.core.board.post.NewsPostPort;
import dev.iamrat.core.board.post.NewsSection;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.ingest.document.application.DocumentIngestResult;
import dev.iamrat.ingest.document.application.IngestDocumentsUseCase;
import dev.iamrat.ingest.news.application.LaunchNewsPublishCommand;
import dev.iamrat.ingest.news.application.PublishDailyDigestUseCase;
import dev.iamrat.ingest.news.application.PublishLaunchNewsUseCase;
import dev.iamrat.source.news.application.NewsSourceClient;
import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.config.import=optional:classpath:application-monitoring.yml")
@ActiveProfiles("test")
class NewsPublishingFlowTest {

    @Autowired private PublishLaunchNewsUseCase publisher;
    @Autowired private PublishDailyDigestUseCase digestPublisher;
    @MockitoBean private NewsSourceClient source;
    @MockitoBean private IngestDocumentsUseCase store;
    @MockitoBean private SearchPort search;
    @MockitoBean private TextGenerationClient model;
    @MockitoBean private NewsPostPort newsPosts;

    @Test
    @DisplayName("적재한 뉴스를 검색해 게시하고 게시글만 데일리로 요약하며 재실행은 중복 게시하지 않는다")
    void ingestsBeforeRagAndPublishesBeforeDailyDigest() {
        List<SourceDocumentCommand> stored = new ArrayList<>();
        List<LaunchNewsPost> launchNews = new ArrayList<>();
        Set<String> digests = new HashSet<>();
        when(source.search(any())).thenReturn(List.of(new NewsSourceItem(
            "갤럭시북 신제품 출시", "신제품에 새 프로세서가 탑재됐다.",
            "https://news.example.com/article/1", "https://news.example.com/article/1",
            "Tue, 08 Sep 2026 09:00:00 +0900"
        )));
        when(store.ingest(any())).thenAnswer(call -> {
            stored.clear();
            stored.addAll(call.<List<SourceDocumentCommand>>getArgument(0));
            return new DocumentIngestResult(stored.size(), stored.size());
        });
        when(search.searchSimilar(anyString(), anyInt())).thenAnswer(call -> {
            assertThat(stored).isNotEmpty();
            return stored.stream().map(SourceDocumentCommand::content).toList();
        });
        when(model.generateForPublishing(anyString(), anyString())).thenReturn("갤럭시북 신제품 출시 소식입니다.");
        when(newsPosts.publishLaunchNews(any())).thenAnswer(call -> {
            launchNews.add(call.getArgument(0));
            return (long) launchNews.size();
        });
        when(newsPosts.isPublished(anyString())).thenAnswer(call -> launchNews.stream()
            .anyMatch(news -> news.canonicalUrl().equals(call.getArgument(0))));
        when(newsPosts.findLaunchNews(any(), any())).thenAnswer(call -> launchNews.stream()
            .filter(news -> news.section() == call.getArgument(0))
            .map(news -> new DailyDigestSourceItem(news.draft().title(), news.draft().summary()))
            .toList());
        when(newsPosts.digestExists(any(), any()))
            .thenAnswer(call -> digests.contains(call.getArgument(0) + "/" + call.getArgument(1)));
        when(newsPosts.publishDailyDigest(any(), any(), any())).thenAnswer(call -> {
            digests.add(call.getArgument(0) + "/" + call.getArgument(1));
            return 100L + digests.size();
        });

        LaunchNewsPublishCommand command = new LaunchNewsPublishCommand(
            "갤럭시북", 5, 3, List.of("출시"), NewsSection.TECHNOLOGY, PostPublishOrigin.SYSTEM_BATCH
        );
        publisher.publish(command);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(model).generateForPublishing(anyString(), prompt.capture());
        assertThat(prompt.getValue()).contains(stored.getFirst().content());
        verify(source).search(any(NewsSourceQuery.class));
        assertThat(launchNews).extracting(LaunchNewsPost::section).containsExactly(NewsSection.TECHNOLOGY);

        digestPublisher.publish(LocalDate.of(2026, 9, 8));
        verify(source).search(any(NewsSourceQuery.class));
        verify(search).searchSimilar(anyString(), anyInt());
        verify(newsPosts).publishDailyDigest(any(), any(), any());

        publisher.publish(command);
        digestPublisher.publish(LocalDate.of(2026, 9, 8));
        assertThat(launchNews).hasSize(1);
        verify(newsPosts).publishDailyDigest(any(), any(), any());
        verify(model, times(2)).generateForPublishing(anyString(), anyString());
    }
}
