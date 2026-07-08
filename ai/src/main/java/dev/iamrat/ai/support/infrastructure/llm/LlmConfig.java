package dev.iamrat.ai.support.infrastructure.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
@RequiredArgsConstructor
public class LlmConfig {

    private final LlmProperties llmProperties;

    @Bean
    @Qualifier("llmChatApi")
    public OpenAiApi llmChatApi() {
        return compatibleApi(
            llmProperties.getChat().getBaseUrl(),
            llmProperties.chatApiKey()
        );
    }

    @Bean
    @Qualifier("llmEmbeddingApi")
    public OpenAiApi llmEmbeddingApi() {
        return compatibleApi(
            llmProperties.getEmbedding().getBaseUrl(),
            llmProperties.embeddingApiKey()
        );
    }

    @Bean
    public OpenAiChatModel llmChatModel(@Qualifier("llmChatApi") OpenAiApi compatibleApi) {
        return OpenAiChatModel.builder()
            .openAiApi(compatibleApi)
            .defaultOptions(OpenAiChatOptions.builder()
                .model(llmProperties.getChat().getOptions().getModel())
                .build())
            .build();
    }

    @Bean
    public OpenAiEmbeddingModel llmEmbeddingModel(@Qualifier("llmEmbeddingApi") OpenAiApi compatibleApi) {
        OpenAiEmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
            .model(llmProperties.getEmbedding().getOptions().getModel())
            .dimensions(llmProperties.getEmbedding().getOptions().getDimensions())
            .build();
        return new OpenAiEmbeddingModel(compatibleApi, MetadataMode.EMBED, embeddingOptions);
    }

    private OpenAiApi compatibleApi(String baseUrl, String apiKey) {
        OpenAiApi.Builder builder = OpenAiApi.builder()
            .baseUrl(baseUrl);
        if (StringUtils.hasText(apiKey)) {
            return builder.apiKey(apiKey).build();
        }
        return builder.apiKey(new NoopApiKey()).build();
    }
}
