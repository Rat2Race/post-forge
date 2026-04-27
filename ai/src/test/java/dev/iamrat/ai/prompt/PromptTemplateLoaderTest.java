package dev.iamrat.ai.prompt;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptTemplateLoaderTest {

    private final PromptTemplateLoader promptTemplateLoader = new PromptTemplateLoader();

    @Test
    @DisplayName("markdown 프롬프트 템플릿을 로드하고 placeholder를 치환한다")
    void render_replacesPlaceholders() {
        String rendered = promptTemplateLoader.render(
            "prompts/chat-system.md",
            Map.of("contextSuffix", "\n\n참고할 컨텍스트:\n테스트 컨텍스트")
        );

        assertThat(rendered)
            .contains("PostForge 커뮤니티의 AI 어시스턴트")
            .contains("테스트 컨텍스트")
            .doesNotContain("{{contextSuffix}}");
    }

    @Test
    @DisplayName("배포된 모든 프롬프트 markdown 리소스를 읽을 수 있다")
    void load_allPromptResources_succeeds() {
        assertThat(promptTemplateLoader.load("prompts/chat-system.md")).isNotBlank();
        assertThat(promptTemplateLoader.load("prompts/news-analysis-system.md")).isNotBlank();
        assertThat(promptTemplateLoader.load("prompts/news-analysis-user.md")).isNotBlank();
    }

    @Test
    @DisplayName("없는 프롬프트 리소스를 읽으면 예외를 던진다")
    void load_missingResource_throws() {
        assertThatThrownBy(() -> promptTemplateLoader.load("prompts/missing.md"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Prompt resource not found");
    }
}
