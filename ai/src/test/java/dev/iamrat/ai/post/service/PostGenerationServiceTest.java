package dev.iamrat.ai.post.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
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
        @DisplayName("뉴스 분석 프롬프트는 markdown 템플릿을 읽어 정확히 조립한다")
        void generateNewsAnalysis_promptTemplates_matchExpectedText() {
            Document relatedNews = new Document("관련 뉴스 내용", Map.of("source", "naver-news"));

            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(relatedNews))
                .willReturn(List.of());

            given(chatModel.call(any(Prompt.class))
                .getResult().getOutput().getText()).willReturn(VALID_JSON_RESPONSE);

            postGenerationService.generateNewsAnalysis(
                "테크",
                "AI 반도체 수요 증가",
                "기사 본문",
                "https://news.example/1"
            );

            ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
            verify(chatModel, atLeastOnce()).call(captor.capture());

            Prompt prompt = captor.getAllValues().getLast();
            SystemMessage systemMessage = (SystemMessage) prompt.getInstructions().getFirst();
            UserMessage userMessage = (UserMessage) prompt.getInstructions().get(1);

            assertThat(systemMessage.getText()).isEqualTo("""
                당신은 뉴스/이슈 트렌드 해설형 AI 분석가입니다.
                
                [분석 방법]
                1. 기사에 나온 핵심 사실을 먼저 정리한다
                2. 기사 자체의 주장과 해석을 구분한다
                3. 과장 없이, 확산 가능성과 불확실성을 함께 설명한다
                4. 기사 원문과 최근 유사 뉴스 문맥을 바탕으로 짧은 트렌드 분석 글을 작성한다
                
                [규칙]
                - 선정적 표현 금지
                - 기사에 없는 사실을 단정하지 말 것
                - 요약, 핵심 포인트, 확산 신호, 리스크, 출처를 포함할 것
                - 반드시 뉴스 링크를 출처에 포함할 것
                
                [응답 형식]
                반드시 아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 포함하지 마세요:
                {
                  \"title\": \"게시글 제목\",
                  \"summary\": \"1-2줄 요약\",
                  \"content\": \"마크다운 형식의 본문\",
                  \"tags\": [\"태그1\", \"태그2\", \"태그3\"]
                }""");
            assertThat(userMessage.getText()).isEqualTo("""
                ## 뉴스 주제
                - 키워드: 테크
                - 기사 제목: AI 반도체 수요 증가
                - 출처 링크: https://news.example/1
                
                ## 신규 기사 본문
                기사 본문
                
                ## 관련 뉴스 묶음
                관련 뉴스 내용
                
                위 내용을 바탕으로 AI 분석 카테고리용 뉴스 해설 글을 작성해주세요.""");
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
