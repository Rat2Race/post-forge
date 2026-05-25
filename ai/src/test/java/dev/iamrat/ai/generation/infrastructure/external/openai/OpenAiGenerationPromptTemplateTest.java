package dev.iamrat.ai.generation.infrastructure.external.openai;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.ai.support.infrastructure.openai.PromptTemplateLoader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenAiGenerationPromptTemplateTest {

    private final OpenAiGenerationPromptTemplate promptTemplate =
        new OpenAiGenerationPromptTemplate(new PromptTemplateLoader());

    @Test
    @DisplayName("뉴스 분석 사용자 프롬프트에 기사와 검색 섹션을 렌더링한다")
    void newsAnalysisUserPrompt_rendersArticleAndSearchSections() {
        String prompt = promptTemplate.newsAnalysisUserPrompt(
            "AI",
            "AI 반도체 수요 증가",
            "기사 본문",
            "https://example.com/news",
            List.of(new SearchResult("관련 뉴스", Map.of())),
            List.of(new SearchResult("과거 분석", Map.of()))
        );

        assertThat(prompt)
            .contains("- 키워드: AI")
            .contains("- 기사 제목: AI 반도체 수요 증가")
            .contains("- 출처 링크: https://example.com/news")
            .contains("기사 본문")
            .contains("## 관련 뉴스 묶음")
            .contains("관련 뉴스")
            .contains("## 과거 유사 뉴스/분석")
            .contains("과거 분석")
            .doesNotContain("{{");
    }
}
