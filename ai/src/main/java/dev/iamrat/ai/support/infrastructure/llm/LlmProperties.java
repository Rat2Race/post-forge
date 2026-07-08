package dev.iamrat.ai.support.infrastructure.llm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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

    @Getter
    @Setter
    public static class Chat {

        @NotBlank
        private String baseUrl = "http://localhost:8088";

        private String apiKey = "";

        @Valid
        private ChatOptions options = new ChatOptions();
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

        @NotBlank
        private String baseUrl = "https://api.openai.com";

        private String apiKey = "";

        @Valid
        private EmbeddingOptions options = new EmbeddingOptions();
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
