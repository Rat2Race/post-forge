package dev.iamrat.ai.support.infrastructure.openai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiPropertiesTest {

    @Test
    @DisplayName("chat과 embedding API key는 공용 key를 기본값으로 쓰되 개별 override가 가능하다")
    void openAiProperties_resolvesApiKeyOverrides() {
        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("shared-key");
        properties.getEmbedding().setApiKey("embedding-key");

        assertThat(properties.chatApiKey()).isEqualTo("shared-key");
        assertThat(properties.embeddingApiKey()).isEqualTo("embedding-key");
    }
}
