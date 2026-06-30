package dev.iamrat.ai.support.infrastructure.openai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiConfigTest {

    @Test
    @DisplayName("OpenAI 공용 설정은 chat과 embedding용 OpenAI-compatible client를 분리해 만든다")
    void openAiConfig_buildsSeparateChatAndEmbeddingModels() {
        OpenAiProperties properties = new OpenAiProperties();
        properties.getChat().getOptions().setModel("chat-model");
        OpenAiConfig config = new OpenAiConfig(properties);

        OpenAiApi chatApi = config.openAiChatApi();
        OpenAiApi embeddingApi = config.openAiEmbeddingApi();
        OpenAiChatModel chatModel = config.openAiChatModel(chatApi);
        OpenAiEmbeddingModel embeddingModel = config.openAiEmbeddingModel(embeddingApi);

        assertThat(chatApi).isNotSameAs(embeddingApi);
        assertThat(chatModel.getDefaultOptions().getModel()).isEqualTo("chat-model");
        assertThat(embeddingModel).isNotNull();
    }
}
