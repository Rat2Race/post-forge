package dev.iamrat.ai.support.infrastructure.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;

import static org.assertj.core.api.Assertions.assertThat;

class LlmConfigTest {

    @Test
    @DisplayName("LLM 설정은 chat과 embedding용 compatible client를 분리해 만든다")
    void llmConfig_buildsSeparateChatAndEmbeddingModels() {
        LlmProperties properties = new LlmProperties();
        properties.getChat().getOptions().setModel("chat-model");
        LlmConfig config = new LlmConfig(properties);

        OpenAiApi chatApi = config.llmChatApi();
        OpenAiApi embeddingApi = config.llmEmbeddingApi();
        OpenAiChatModel chatModel = config.llmChatModel(chatApi);
        OpenAiEmbeddingModel embeddingModel = config.llmEmbeddingModel(embeddingApi);

        assertThat(chatApi).isNotSameAs(embeddingApi);
        assertThat(chatModel.getDefaultOptions().getModel()).isEqualTo("chat-model");
        assertThat(embeddingModel).isNotNull();
    }
}
