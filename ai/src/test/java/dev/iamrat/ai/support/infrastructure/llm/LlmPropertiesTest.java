package dev.iamrat.ai.support.infrastructure.llm;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmPropertiesTest {

    @Test
    @DisplayName("chat API key는 공용 key를 기본값으로 쓰되 개별 override가 가능하다")
    void llmProperties_resolvesApiKeyOverrides() {
        LlmProperties properties = new LlmProperties();
        properties.setApiKey("shared-key");

        assertThat(properties.chatApiKey()).isEqualTo("shared-key");

        properties.getChat().setApiKey("chat-key");

        assertThat(properties.chatApiKey()).isEqualTo("chat-key");
    }

    @Test
    @DisplayName("chat HTTP timeout 기본값을 제공하고 0 이하 값은 기본값으로 되돌린다")
    void llmProperties_normalizesTimeouts() {
        LlmProperties properties = new LlmProperties();

        assertThat(properties.getChat().getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getChat().getReadTimeout()).isEqualTo(Duration.ofSeconds(60));

        properties.getChat().setReadTimeout(Duration.ofSeconds(120));

        assertThat(properties.getChat().getReadTimeout()).isEqualTo(Duration.ofSeconds(120));

        properties.getChat().setReadTimeout(Duration.ZERO);

        assertThat(properties.getChat().getReadTimeout()).isEqualTo(Duration.ofSeconds(60));
    }
}
