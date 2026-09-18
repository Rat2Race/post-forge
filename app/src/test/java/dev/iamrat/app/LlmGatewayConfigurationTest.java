package dev.iamrat.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

class LlmGatewayConfigurationTest {

    @Test
    void localDefaultsUseGatewayForBothModels() throws IOException {
        MockEnvironment env = load("application.yml");

        assertThat(env.getProperty("app.llm.chat.base-url")).isEqualTo("http://localhost:8088");
        assertThat(env.getProperty("app.llm.embedding.base-url")).isEqualTo("http://localhost:8088");
        assertThat(env.getProperty("app.llm.embedding.options.model")).isEqualTo("bge-m3");
        assertThat(env.getProperty("app.llm.embedding.options.dimensions")).isEqualTo("1024");
    }

    @ParameterizedTest
    @ValueSource(strings = {"application.yml", "application-prod.yml"})
    void bothProfilesUseGatewayTokenUnlessEmbeddingKeyOverridesIt(String resource) throws IOException {
        MockEnvironment env = load(resource)
            .withProperty("LLM_CHAT_BASE_URL", "http://10.0.0.1:8088")
            .withProperty("LLM_EMBEDDING_BASE_URL", "http://10.0.0.1:8088")
            .withProperty("LLM_GATEWAY_TOKEN", "test-gateway-token")
            .withProperty("OPENAI_API_KEY", "test-openai-key");

        assertThat(env.getProperty("app.llm.embedding.base-url"))
            .isEqualTo(env.getProperty("app.llm.chat.base-url"));
        assertThat(env.getProperty("app.llm.chat.api-key")).isEqualTo("test-gateway-token");
        assertThat(env.getProperty("app.llm.embedding.api-key")).isEqualTo("test-gateway-token");

        env.withProperty("LLM_EMBEDDING_API_KEY", "test-embedding-key");
        assertThat(env.getProperty("app.llm.embedding.api-key")).isEqualTo("test-embedding-key");
        assertThat(env.getProperty("app.llm.chat.api-key")).isEqualTo("test-gateway-token");
    }

    @ParameterizedTest
    @ValueSource(strings = {"application.yml", "application-prod.yml"})
    void explicitEmbeddingProviderAndLegacyOpenAiKeyRemainSupported(String resource) throws IOException {
        MockEnvironment env = load(resource)
            .withProperty("LLM_EMBEDDING_BASE_URL", "https://api.openai.com")
            .withProperty("LLM_EMBEDDING_MODEL", "text-embedding-3-small")
            .withProperty("OPENAI_API_KEY", "test-openai-key");

        assertThat(env.getProperty("app.llm.embedding.base-url")).isEqualTo("https://api.openai.com");
        assertThat(env.getProperty("app.llm.embedding.api-key")).isEqualTo("test-openai-key");
        assertThat(env.getProperty("app.llm.embedding.options.model")).isEqualTo("text-embedding-3-small");
        assertThat(env.getProperty("app.llm.embedding.options.dimensions")).isEqualTo("1024");
    }

    private MockEnvironment load(String resource) throws IOException {
        MockEnvironment env = new MockEnvironment();
        new YamlPropertySourceLoader().load(resource, new ClassPathResource(resource))
            .forEach(source -> env.getPropertySources().addLast(source));
        return env;
    }
}
