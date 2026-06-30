package dev.iamrat.ai.support.infrastructure.llm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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

    @Valid
    private Embedding embedding = new Embedding();

    public String chatApiKey() {
        return firstNonBlank(chat.getApiKey(), apiKey);
    }

    public String embeddingApiKey() {
        return firstNonBlank(embedding.getApiKey(), apiKey);
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
        private String baseUrl = "http://localhost:8088";

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
        private String model = "qwen2.5-coder:7b";
    }

    @Getter
    @Setter
    public static class Embedding {

        private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
        private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

        @NotBlank
        private String baseUrl = "https://api.openai.com";

        private String apiKey = "";

        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;

        private Duration readTimeout = DEFAULT_READ_TIMEOUT;

        @Valid
        private EmbeddingOptions options = new EmbeddingOptions();

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = positiveOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = positiveOrDefault(readTimeout, DEFAULT_READ_TIMEOUT);
        }
    }

    @Getter
    @Setter
    public static class EmbeddingOptions {

        @NotBlank
        private String model = "text-embedding-ada-002";

        @Positive
        private Integer dimensions;
    }
}
