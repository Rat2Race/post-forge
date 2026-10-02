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
        String prompt = loader.load("prompts/study-question-system.md");

        assertThat(prompt)
            .startsWith("너는 학습자가 올린 자료로 복습 문제를 만드는 출제자다.")
            .doesNotStartWith("\n")
            .doesNotEndWith("\n");
    }

    @Test
    @DisplayName("학습 프롬프트 리소스를 모두 읽을 수 있다")
    void load_readsStudyPromptResources() {
        assertThat(loader.load("prompts/study-question-system.md")).contains("복습 문제를 만드는 출제자");
        assertThat(loader.load("prompts/study-student-system.md")).isNotBlank();
        assertThat(loader.load("prompts/study-follow-up-system.md")).contains("꼬리질문");
    }

    @Test
    @DisplayName("시작 시 읽은 프롬프트 문자열을 재사용한다")
    void load_reusesEagerlyLoadedPrompt() {
        assertThat(loader.load("prompts/study-question-system.md"))
            .isSameAs(loader.load("prompts/study-question-system.md"));
    }

    @Test
    @DisplayName("프롬프트 placeholder를 치환하고 null 값은 빈 문자열로 렌더링한다")
    void render_replacesPlaceholdersAndNullValues() {
        Map<String, String> values = new HashMap<>();
        values.put("limit", null);

        String rendered = loader.render(
            "prompts/study-question-system.md",
            values
        );

        assertThat(rendered)
            .contains("복습 문제를 만드는 출제자")
            .doesNotContain("{{limit}}")
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
