package dev.iamrat.ai.post.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.post.NewsAnalysisPostRequest;
import dev.iamrat.ai.post.dto.GeneratedPost;
import dev.iamrat.ai.prompt.PromptTemplateLoader;
import dev.iamrat.board.post.PostCategory;
import dev.iamrat.board.post.PostWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostGenerationServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatModel chatModel;

    @Mock
    private VectorStore vectorStore;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PostWriter postWriter;

    @Spy
    private OutputGuardrail outputGuardrail = new OutputGuardrail();

    @Spy
    private PromptTemplateLoader promptTemplateLoader = new PromptTemplateLoader();

    @InjectMocks
    private PostGenerationService postGenerationService;

    private static final String VALID_JSON_RESPONSE = """
            {
              \"title\": \"오늘의 트렌드 분석\",
              \"summary\": \"핵심 흐름 요약\",
              \"content\": \"## 요약\\n오늘의 트렌드를 정리합니다\",
              \"tags\": [\"트렌드\", \"뉴스\"]
            }
            """;

    @Nested
    @DisplayName("뉴스 분석 게시글 생성")
    class GenerateTests {

        @Test
        @DisplayName("관련 뉴스 문맥과 LLM 응답으로 뉴스 분석 게시글을 생성한다")
        void generateNewsAnalysis_withNewsContext_returnsGeneratedPost() {
            Document relatedNews = new Document("관련 뉴스 내용", Map.of("source", "naver-news"));

            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(relatedNews))
                .willReturn(List.of());

            given(chatModel.call(any(Prompt.class))
                .getResult().getOutput().getText()).willReturn(VALID_JSON_RESPONSE);

            GeneratedPost result = postGenerationService.generateNewsAnalysis(
                "테크",
                "AI 반도체 수요 증가",
                "기사 본문",
                "https://news.example/1"
            );

            assertThat(result.title()).isEqualTo("오늘의 트렌드 분석");
            assertThat(result.content()).contains("사실 관계와 맥락을 다시 확인하세요");
            verify(vectorStore, times(2)).similaritySearch(any(SearchRequest.class));
        }

        @Test
        @DisplayName("LLM 응답 파싱 실패 시 기본 형식으로 변환한다")
        void generateNewsAnalysis_invalidJsonResponse_fallsBackToDefault() {
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of())
                .willReturn(List.of());
            given(chatModel.call(any(Prompt.class))
                .getResult().getOutput().getText()).willReturn("일반 텍스트 응답");

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
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of())
                .willReturn(List.of());
            given(chatModel.call(any(Prompt.class))
                .getResult().getOutput().getText()).willReturn(VALID_JSON_RESPONSE);
            given(postWriter.write(
                eq("오늘의 트렌드 분석"),
                contains("사실 관계와 맥락을 다시 확인하세요"),
                eq("핵심 흐름 요약"),
                eq(List.of("트렌드", "뉴스")),
                eq("ai-post-generator"),
                eq("AI 분석가"),
                eq(PostCategory.AI_ANALYSIS)
            )).willReturn(42L);

            Long postId = postGenerationService.publishNewsAnalysis(new NewsAnalysisPostRequest(
                "테크",
                "AI 반도체 수요 증가",
                "기사 본문",
                "https://news.example/1"
            ));

            assertThat(postId).isEqualTo(42L);
        }

        @Test
        @DisplayName("PostWriter를 통해 AI_ANALYSIS 카테고리로 게시글을 등록한다")
        void publish_callsPostWriter_returnsId() {
            GeneratedPost post = new GeneratedPost(
                "제목", "요약", "내용", List.of("태그"));
            given(postWriter.write("제목", "내용", "요약", List.of("태그"),
                "ai-post-generator", "AI 분석가", PostCategory.AI_ANALYSIS)).willReturn(42L);

            Long postId = postGenerationService.publish(post);

            assertThat(postId).isEqualTo(42L);
            verify(postWriter).write("제목", "내용", "요약", List.of("태그"),
                "ai-post-generator", "AI 분석가", PostCategory.AI_ANALYSIS);
        }
    }
}
