package dev.iamrat.ai.support.infrastructure.openai;

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
@EnableConfigurationProperties(OpenAiProperties.class)
@RequiredArgsConstructor
public class OpenAiConfig {

    private final OpenAiProperties openAiProperties;

    @Bean
    @Qualifier("openAiChatApi")
    public OpenAiApi openAiChatApi() {
        return openAiApi(
            openAiProperties.getChat().getBaseUrl(),
            openAiProperties.chatApiKey()
        );
    }

    @Bean
    @Qualifier("openAiEmbeddingApi")
    public OpenAiApi openAiEmbeddingApi() {
        return openAiApi(
            openAiProperties.getEmbedding().getBaseUrl(),
            openAiProperties.embeddingApiKey()
        );
    }

    @Bean
    public OpenAiChatModel openAiChatModel(@Qualifier("openAiChatApi") OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(OpenAiChatOptions.builder()
                .model(openAiProperties.getChat().getOptions().getModel())
                .build())
            .build();
    }

    @Bean
    public OpenAiEmbeddingModel openAiEmbeddingModel(@Qualifier("openAiEmbeddingApi") OpenAiApi openAiApi) {
        OpenAiEmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
            .model(openAiProperties.getEmbedding().getOptions().getModel())
            .dimensions(openAiProperties.getEmbedding().getOptions().getDimensions())
            .build();
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, embeddingOptions);
    }

    private OpenAiApi openAiApi(String baseUrl, String apiKey) {
        OpenAiApi.Builder builder = OpenAiApi.builder()
            .baseUrl(baseUrl);
        if (StringUtils.hasText(apiKey)) {
            return builder.apiKey(apiKey).build();
        }
        return builder.apiKey(new NoopApiKey()).build();
    }
}
