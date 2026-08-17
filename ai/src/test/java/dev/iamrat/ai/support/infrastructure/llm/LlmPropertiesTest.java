package dev.iamrat.ai.support.infrastructure.llm;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmPropertiesTest {

    @Test
    @DisplayName("chat과 embedding API key는 공용 key를 기본값으로 쓰되 개별 override가 가능하다")
    void llmProperties_resolvesApiKeyOverrides() {
        LlmProperties properties = new LlmProperties();
        properties.setApiKey("shared-key");
        properties.getEmbedding().setApiKey("embedding-key");

        assertThat(properties.chatApiKey()).isEqualTo("shared-key");
        assertThat(properties.embeddingApiKey()).isEqualTo("embedding-key");
    }

    @Test
    @DisplayName("chat과 embedding HTTP timeout 기본값을 제공하고 0 이하 값은 기본값으로 되돌린다")
    void llmProperties_normalizesTimeouts() {
        LlmProperties properties = new LlmProperties();

        assertThat(properties.getChat().getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getChat().getReadTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.getEmbedding().getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getEmbedding().getReadTimeout()).isEqualTo(Duration.ofSeconds(30));

        properties.getChat().setReadTimeout(Duration.ofSeconds(120));
        properties.getEmbedding().setConnectTimeout(Duration.ofSeconds(5));

        assertThat(properties.getChat().getReadTimeout()).isEqualTo(Duration.ofSeconds(120));
        assertThat(properties.getEmbedding().getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));

        properties.getChat().setReadTimeout(Duration.ZERO);
        properties.getEmbedding().setConnectTimeout(null);

        assertThat(properties.getChat().getReadTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.getEmbedding().getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
    }
}
