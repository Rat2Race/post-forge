package dev.iamrat.ai.generation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.generation.domain.GeneratedPost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedPostParserTest {

    private final GeneratedPostParser parser = new GeneratedPostParser(new ObjectMapper());

    @Test
    @DisplayName("JSON 코드블록 응답을 GeneratedPost로 파싱한다")
    void parse_jsonCodeBlock_returnsGeneratedPost() {
        String response = """
            ```json
            {
              "title": "오늘의 트렌드 분석",
              "summary": "핵심 흐름 요약",
              "content": "본문",
              "tags": ["트렌드", "뉴스"]
            }
            ```
            """;

        GeneratedPost post = parser.parse(response);

        assertThat(post.title()).isEqualTo("오늘의 트렌드 분석");
        assertThat(post.summary()).isEqualTo("핵심 흐름 요약");
        assertThat(post.content()).isEqualTo("본문");
        assertThat(post.tags()).containsExactly("트렌드", "뉴스");
    }

    @Test
    @DisplayName("JSON 파싱 실패 시 기본 GeneratedPost로 변환한다")
    void parse_invalidJson_returnsFallbackPost() {
        GeneratedPost post = parser.parse("일반 텍스트 응답");

        assertThat(post.title()).isEqualTo("트렌드 분석");
        assertThat(post.summary()).isEqualTo("일반 텍스트 응답");
        assertThat(post.content()).isEqualTo("일반 텍스트 응답");
        assertThat(post.tags()).isEmpty();
    }

    @Test
    @DisplayName("fallback summary는 100자를 넘으면 말줄임 처리한다")
    void parse_longInvalidJson_truncatesFallbackSummary() {
        String response = "가".repeat(101);

        GeneratedPost post = parser.parse(response);

        assertThat(post.summary()).hasSize(103);
        assertThat(post.summary()).endsWith("...");
    }
}
