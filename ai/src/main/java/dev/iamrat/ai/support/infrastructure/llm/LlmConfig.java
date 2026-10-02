package dev.iamrat.ai.support.infrastructure.llm;

import java.net.http.HttpClient;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

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
            llmProperties.chatApiKey(),
            llmProperties.getChat().getConnectTimeout(),
            llmProperties.getChat().getReadTimeout()
        );
    }

    @Bean
    @Primary
    public OpenAiChatModel llmChatModel(@Qualifier("llmChatApi") OpenAiApi compatibleApi) {
        return OpenAiChatModel.builder()
            .openAiApi(compatibleApi)
            .defaultOptions(chatOptions())
            // Spring AI 기본값은 실패를 간격을 늘려 가며 10번까지 다시 보낸다. 생성 한 번이 수십 초라 재시도가 쌓이면 프록시 제한(524)을 넘는다.
            // 모델은 한 번만 보내고, 실패 처리(규칙 문제로 대체 등)는 호출자가 정한다.
            .retryTemplate(RetryTemplate.builder().maxAttempts(1).build())
            .build();
    }

    private OpenAiChatOptions chatOptions() {
        LlmProperties.ChatOptions options = llmProperties.getChat().getOptions();
        String reasoningEffort = options.getReasoningEffort();
        return OpenAiChatOptions.builder()
            .model(options.getModel())
            .reasoningEffort(StringUtils.hasText(reasoningEffort) ? reasoningEffort : null)
            .build();
    }

    private OpenAiApi compatibleApi(String baseUrl, String apiKey, Duration connectTimeout, Duration readTimeout) {
        OpenAiApi.Builder builder = OpenAiApi.builder()
            .baseUrl(baseUrl)
            .restClientBuilder(RestClient.builder()
                .requestFactory(timeoutRequestFactory(connectTimeout, readTimeout)));
        if (StringUtils.hasText(apiKey)) {
            return builder.apiKey(apiKey).build();
        }
        return builder.apiKey(new NoopApiKey()).build();
    }

    private ClientHttpRequestFactory timeoutRequestFactory(Duration connectTimeout, Duration readTimeout) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                // The gateway's HTTP parser mishandles h2c upgrades; use HTTP/1.1 for both models.
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build()
        );
        requestFactory.setReadTimeout(readTimeout);
        return requestFactory;
    }
}
