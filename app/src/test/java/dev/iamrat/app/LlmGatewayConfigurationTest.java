package dev.iamrat.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

/**
 * application.yml(공통) 위에 application-{profile}.yml(차이만)이 덮이는 계층의 LLM 계약.
 * prod 파일은 레포에 없으므로(.gitignore) 공통 파일과 local 파일만 검사한다.
 */
class LlmGatewayConfigurationTest {

    @Test
    void localProfileDefaultsToOllama() throws IOException {
        MockEnvironment env = load("application-local.yml");

        assertThat(env.getProperty("app.llm.chat.base-url")).isEqualTo("http://localhost:11434");
        assertThat(env.getProperty("app.llm.chat.options.model")).isEqualTo("qwen3:8b");
    }

    @Test
    void commonConfigHasNoGatewayDefaultSoProdFailsFastWithoutEnv() throws IOException {
        MockEnvironment env = load("application.yml");

        assertThatThrownBy(() -> env.getProperty("app.llm.chat.base-url"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("LLM_GATEWAY_BASE_URL");
        assertThatThrownBy(() -> env.getProperty("app.llm.chat.options.model"))
            .hasMessageContaining("LLM_CHAT_MODEL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"application.yml", "application-local.yml"})
    void gatewayAppliesToChatAndChatOverridesWin(String topmost) throws IOException {
        MockEnvironment env = load(topmost)
            .withProperty("LLM_GATEWAY_BASE_URL", "http://10.0.0.1:8088")
            .withProperty("LLM_GATEWAY_TOKEN", "test-gateway-token");

        assertThat(env.getProperty("app.llm.chat.base-url")).isEqualTo("http://10.0.0.1:8088");
        assertThat(env.getProperty("app.llm.chat.api-key")).isEqualTo("test-gateway-token");

        env.withProperty("LLM_CHAT_BASE_URL", "https://api.openai.com")
            .withProperty("LLM_CHAT_API_KEY", "test-chat-key");
        assertThat(env.getProperty("app.llm.chat.base-url")).isEqualTo("https://api.openai.com");
        assertThat(env.getProperty("app.llm.chat.api-key")).isEqualTo("test-chat-key");
    }

    /** 우선순위: withProperty(환경변수 역할) > 프로필 파일 > application.yml. 실제 기동 순서와 같다. */
    private MockEnvironment load(String topmost) throws IOException {
        MockEnvironment env = new MockEnvironment();
        if (!"application.yml".equals(topmost)) {
            addLast(env, topmost);
        }
        addLast(env, "application.yml");
        return env;
    }

    private void addLast(MockEnvironment env, String resource) throws IOException {
        new YamlPropertySourceLoader().load(resource, new ClassPathResource(resource))
            .forEach(source -> env.getPropertySources().addLast(source));
    }
}
