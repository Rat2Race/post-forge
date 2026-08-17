package dev.iamrat.ai.support.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptResourceLoaderTest {

    private final PromptResourceLoader loader = new PromptResourceLoader();

    @Test
    @DisplayName("프롬프트 리소스를 읽고 앞뒤 공백을 제거한다")
    void load_readsPromptResourceAndTrims() {
        String prompt = loader.load("prompts/chat-system.md");

        assertThat(prompt)
            .startsWith("당신은 PostForge 커뮤니티의 AI 어시스턴트입니다.")
            .doesNotStartWith("\n")
            .doesNotEndWith("\n");
    }

    @Test
    @DisplayName("공통 AI 프롬프트 리소스를 모두 읽을 수 있다")
    void load_readsCommonAiPromptResources() {
        assertThat(loader.load("prompts/safety-policy.md")).contains("공통 보안 정책");
        assertThat(loader.load("prompts/launch-news-draft-system.md"))
            .contains("verified new-product launch news article");
        assertThat(loader.load("prompts/news-analysis-system.md")).isNotBlank();
        assertThat(loader.load("prompts/news-analysis-user.md")).isNotBlank();
        assertThat(loader.load("prompts/refusal-security.md")).contains("보안상 민감한 정보");
    }

    @Test
    @DisplayName("시작 시 읽은 프롬프트 문자열을 재사용한다")
    void load_reusesEagerlyLoadedPrompt() {
        assertThat(loader.load("prompts/chat-system.md"))
            .isSameAs(loader.load("prompts/chat-system.md"));
    }

    @Test
    @DisplayName("프롬프트 placeholder를 치환하고 null 값은 빈 문자열로 렌더링한다")
    void render_replacesPlaceholdersAndNullValues() {
        Map<String, String> values = new HashMap<>();
        values.put("contextSuffix", null);

        String rendered = loader.render(
            "prompts/chat-system.md",
            values
        );

        assertThat(rendered)
            .contains("당신은 PostForge 커뮤니티의 AI 어시스턴트입니다.")
            .doesNotContain("{{contextSuffix}}")
            .doesNotEndWith("\n");
    }

    @Test
    @DisplayName("없는 프롬프트 리소스는 명확한 예외를 던진다")
    void load_throwsWhenPromptResourceIsMissing() {
        assertThatThrownBy(() -> loader.load("prompts/missing.md"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("프롬프트 리소스를 찾을 수 없습니다: prompts/missing.md");
    }
}
