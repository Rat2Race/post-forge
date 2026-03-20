package dev.iamrat.ai.post.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.post.dto.GeneratedPost;
import dev.iamrat.post.PostWriter;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private PostGenerationService postGenerationService;

    private static final String VALID_JSON_RESPONSE = """
            {
              "title": "삼성전자 2025년 사업보고서 분석",
              "summary": "삼성전자 매출액 전년 대비 9.8% 증가",
              "content": "## 실적 요약\\n매출액 78조원으로 전년 대비 증가",
              "tags": ["삼성전자", "실적분석", "DART"]
            }
            """;

    @Nested
    @DisplayName("게시글 생성")
    class GenerateTests {

        @Test
        @DisplayName("벡터 검색 결과와 LLM 응답으로 게시글을 생성한다")
        void generate_withValidData_returnsGeneratedPost() {
            // given
            Document dartDoc = new Document("DART 공시 내용",
                    Map.of("source", "dart", "corpName", "삼성전자", "stockCode", "005930"));
            Document financialDoc = new Document("재무 수치 내용",
                    Map.of("source", "dart-financial", "stockCode", "005930"));
            Document newsDoc = new Document("뉴스 내용",
                    Map.of("source", "naver-news"));

            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of(dartDoc))       // dart
                    .willReturn(List.of(financialDoc))   // dart-financial
                    .willReturn(List.of(newsDoc))        // naver-news
                    .willReturn(List.of());              // history

            given(chatModel.call(any(Prompt.class))
                    .getResult().getOutput().getText()).willReturn(VALID_JSON_RESPONSE);

            // when
            GeneratedPost result = postGenerationService.generate("005930", "삼성전자");

            // then
            assertThat(result.title()).isEqualTo("삼성전자 2025년 사업보고서 분석");
            assertThat(result.summary()).contains("매출액");
            assertThat(result.tags()).containsExactly("삼성전자", "실적분석", "DART");
            verify(vectorStore, times(4)).similaritySearch(any(SearchRequest.class));
        }

        @Test
        @DisplayName("corpName이 null이면 공시 메타데이터에서 추출한다")
        void generate_nullCorpName_extractsFromMetadata() {
            // given
            Document dartDoc = new Document("DART 공시 내용",
                    Map.of("source", "dart", "corpName", "삼성전자", "stockCode", "005930"));

            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of(dartDoc))
                    .willReturn(List.of())
                    .willReturn(List.of())
                    .willReturn(List.of());

            given(chatModel.call(any(Prompt.class))
                    .getResult().getOutput().getText()).willReturn(VALID_JSON_RESPONSE);

            // when
            GeneratedPost result = postGenerationService.generate("005930", null);

            // then
            assertThat(result).isNotNull();
            assertThat(result.title()).isNotBlank();
        }

        @Test
        @DisplayName("LLM 응답이 마크다운 코드블록으로 감싸져 있어도 파싱한다")
        void generate_markdownWrappedResponse_parsesCorrectly() {
            // given
            String wrappedResponse = "```json\n" + VALID_JSON_RESPONSE + "\n```";

            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of());

            given(chatModel.call(any(Prompt.class))
                    .getResult().getOutput().getText()).willReturn(wrappedResponse);

            // when
            GeneratedPost result = postGenerationService.generate("005930", "삼성전자");

            // then
            assertThat(result.title()).isEqualTo("삼성전자 2025년 사업보고서 분석");
        }

        @Test
        @DisplayName("LLM 응답 파싱 실패 시 기본 형식으로 변환한다")
        void generate_invalidJsonResponse_fallsBackToDefault() {
            // given
            String invalidResponse = "이것은 JSON이 아닌 일반 텍스트 응답입니다.";

            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of());

            given(chatModel.call(any(Prompt.class))
                    .getResult().getOutput().getText()).willReturn(invalidResponse);

            // when
            GeneratedPost result = postGenerationService.generate("005930", "삼성전자");

            // then
            assertThat(result.title()).isEqualTo("시장 분석");
            assertThat(result.content()).isEqualTo(invalidResponse);
            assertThat(result.tags()).isEmpty();
        }

        @Test
        @DisplayName("벡터 검색 실패 시 빈 결과로 계속 진행한다")
        void generate_vectorSearchFails_continuesWithEmptyResults() {
            // given
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willThrow(new RuntimeException("Vector DB connection failed"));

            given(chatModel.call(any(Prompt.class))
                    .getResult().getOutput().getText()).willReturn(VALID_JSON_RESPONSE);

            // when
            GeneratedPost result = postGenerationService.generate("005930", "삼성전자");

            // then
            assertThat(result).isNotNull();
            assertThat(result.title()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("게시글 발행")
    class PublishTests {

        @Test
        @DisplayName("PostWriter를 통해 게시글을 등록하고 ID를 반환한다")
        void publish_callsPostWriter_returnsId() {
            // given
            GeneratedPost post = new GeneratedPost(
                    "제목", "요약", "내용", List.of("태그"));
            given(postWriter.write("제목", "내용", "ai-post-generator", "AI 분석가")).willReturn(42L);

            // when
            Long postId = postGenerationService.publish(post);

            // then
            assertThat(postId).isEqualTo(42L);
            verify(postWriter).write("제목", "내용", "ai-post-generator", "AI 분석가");
        }
    }
}
