package dev.iamrat.ai.support.infrastructure.llm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private String apiKey = "";

    @NotBlank
    private String provider = "ollama";

    @Valid
    private Chat chat = new Chat();

    public String chatApiKey() {
        return firstNonBlank(chat.getApiKey(), apiKey);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue) {
        if (value == null || value.isZero() || value.isNegative()) {
            return defaultValue;
        }
        return value;
    }

    @Getter
    @Setter
    public static class Chat {

        private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
        private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);

        @NotBlank
        private String baseUrl;

        private String apiKey = "";

        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;

        private Duration readTimeout = DEFAULT_READ_TIMEOUT;

        @Valid
        private ChatOptions options = new ChatOptions();

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = positiveOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = positiveOrDefault(readTimeout, DEFAULT_READ_TIMEOUT);
        }
    }

    @Getter
    @Setter
    public static class ChatOptions {

        @NotBlank
        private String model;

        // 비우면 보내지 않는다. 로컬 qwen3:8b는 none이면 생각 단계를 건너뛰어 같은 요청이 34s에서 13s로 준다.
        private String reasoningEffort;
    }
}
