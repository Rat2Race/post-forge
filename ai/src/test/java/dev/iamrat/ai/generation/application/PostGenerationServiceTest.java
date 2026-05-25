package dev.iamrat.ai.generation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.generation.domain.GeneratedPost;
import dev.iamrat.ai.search.application.SemanticSearchService;
import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.core.ai.post.NewsAnalysisPostRequest;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostWriteCommand;
import dev.iamrat.core.board.post.PostWriter;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostGenerationServiceTest {

    @Mock
    private TextGenerationClient textGenerationClient;

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private PostWriter postWriter;

    private PostGenerationService postGenerationService;

    private static final String VALID_JSON_RESPONSE = """
            {
              \"title\": \"오늘의 트렌드 분석\",
              \"summary\": \"핵심 흐름 요약\",
              \"content\": \"## 요약\\n오늘의 트렌드를 정리합니다\",
              \"tags\": [\"트렌드\", \"뉴스\"]
            }
            """;

    @BeforeEach
    void setUp() {
        postGenerationService = new PostGenerationService(
            postWriter,
            new OutputGuardrail(),
            new GeneratedPostParser(new ObjectMapper()),
            textGenerationClient,
            new TestAiPromptTemplate(),
            semanticSearchService
        );
    }

    @Nested
    @DisplayName("뉴스 분석 게시글 생성")
    class GenerateTests {

        @Test
        @DisplayName("관련 뉴스 문맥과 LLM 응답으로 뉴스 분석 게시글을 생성한다")
        void generateNewsAnalysis_withNewsContext_returnsGeneratedPost() {
            SearchResult relatedNews = new SearchResult(
                "관련 뉴스 내용",
                Map.of(SourceDocumentCommand.SOURCE_METADATA_KEY, NewsDocumentMetadata.SOURCE_NAVER_NEWS)
            );
            given(semanticSearchService.searchBySource(NewsDocumentMetadata.SOURCE_NAVER_NEWS, "테크", 5))
                .willReturn(List.of(relatedNews));
            given(semanticSearchService.searchSimilar("테크 트렌드 분석", 3)).willReturn(List.of());
            given(textGenerationClient.generate(anyString(), anyString())).willReturn(VALID_JSON_RESPONSE);

            GeneratedPost result = postGenerationService.generateNewsAnalysis(
                "테크",
                "AI 반도체 수요 증가",
                "기사 본문",
                "https://news.example/1"
            );

            assertThat(result.title()).isEqualTo("오늘의 트렌드 분석");
            assertThat(result.content()).contains("사실 관계와 맥락을 다시 확인하세요");
            verify(semanticSearchService).searchBySource(NewsDocumentMetadata.SOURCE_NAVER_NEWS, "테크", 5);
            verify(semanticSearchService).searchSimilar("테크 트렌드 분석", 3);

            ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
            verify(textGenerationClient).generate(anyString(), userPromptCaptor.capture());
            assertThat(userPromptCaptor.getValue())
                .contains("관련 뉴스 내용")
                .contains("AI 반도체 수요 증가");
        }

        @Test
        @DisplayName("LLM 응답 파싱 실패 시 기본 형식으로 변환한다")
        void generateNewsAnalysis_invalidJsonResponse_fallsBackToDefault() {
            given(semanticSearchService.searchBySource(NewsDocumentMetadata.SOURCE_NAVER_NEWS, "테크", 5))
                .willReturn(List.of());
            given(semanticSearchService.searchSimilar("테크 트렌드 분석", 3)).willReturn(List.of());
            given(textGenerationClient.generate(anyString(), anyString())).willReturn("일반 텍스트 응답");

            GeneratedPost result = postGenerationService.generateNewsAnalysis(
                "테크",
                "AI 반도체 수요 증가",
                "기사 본문",
                "https://news.example/1"
            );

            assertThat(result.title()).isEqualTo("트렌드 분석");
            assertThat(result.content()).contains("일반 텍스트 응답");
        }
    }

    @Nested
    @DisplayName("게시글 발행")
    class PublishTests {

        @Test
        @DisplayName("뉴스 분석 발행 port를 통해 생성과 게시글 등록을 완료한다")
        void publishNewsAnalysis_generatesAndPublishesPost() {
            given(semanticSearchService.searchBySource(NewsDocumentMetadata.SOURCE_NAVER_NEWS, "테크", 5))
                .willReturn(List.of());
            given(semanticSearchService.searchSimilar("테크 트렌드 분석", 3)).willReturn(List.of());
            given(textGenerationClient.generate(anyString(), anyString())).willReturn(VALID_JSON_RESPONSE);
            given(postWriter.write(argThat(command ->
                command.title().equals("오늘의 트렌드 분석")
                    && command.content().contains("사실 관계와 맥락을 다시 확인하세요")
                    && command.summary().equals("핵심 흐름 요약")
                    && command.tags().equals(List.of("트렌드", "뉴스"))
                    && command.accountId() == null
                    && command.nickname().equals("AI 분석가")
                    && command.category() == PostCategory.AI_ANALYSIS
            ))).willReturn(42L);

            Long postId = postGenerationService.publishNewsAnalysis(new NewsAnalysisPostRequest(
                "테크",
                "AI 반도체 수요 증가",
                "기사 본문",
                "https://news.example/1"
            ));

            assertThat(postId).isEqualTo(42L);
            verify(textGenerationClient, times(1)).generate(anyString(), anyString());
        }

        @Test
        @DisplayName("PostWriter를 통해 AI_ANALYSIS 카테고리로 게시글을 등록한다")
        void publish_callsPostWriter_returnsId() {
            GeneratedPost post = new GeneratedPost(
                "제목", "요약", "내용", List.of("태그"));
            PostWriteCommand command = new PostWriteCommand(
                "제목", "내용", "요약", List.of("태그"),
                null, "AI 분석가", PostCategory.AI_ANALYSIS);
            given(postWriter.write(command)).willReturn(42L);

            Long postId = postGenerationService.publish(post);

            assertThat(postId).isEqualTo(42L);
            verify(postWriter).write(command);
        }
    }
}
