package dev.iamrat.ai.support.infrastructure.llm;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmConfigTest {

    @Test
    @DisplayName("채팅과 임베딩 요청은 HTTP/1.1로 보내고 h2c 업그레이드를 요청하지 않는다")
    void llmClients_sendHttp11WithoutUpgrade() throws Exception {
        var protocols = new ConcurrentLinkedQueue<String>();
        var upgrades = new ConcurrentLinkedQueue<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/", exchange -> {
            protocols.add(exchange.getProtocol());
            String upgrade = exchange.getRequestHeaders().getFirst("Upgrade");
            upgrades.add(upgrade == null ? "" : upgrade);
            exchange.getRequestBody().readAllBytes();
            String json = switch (exchange.getRequestURI().getPath()) {
                case "/v1/chat/completions" -> """
                    {"id":"test","object":"chat.completion","created":1,"model":"chat-model",
                     "choices":[{"index":0,"message":{"role":"assistant","content":"ok"},"finish_reason":"stop"}],
                     "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}
                    """;
                case "/v1/embeddings" -> """
                    {"object":"list","model":"embedding-model",
                     "data":[{"object":"embedding","index":0,"embedding":[0.25,0.5]}],
                     "usage":{"prompt_tokens":1,"total_tokens":1}}
                    """;
                default -> throw new IllegalStateException("Unexpected endpoint: " + exchange.getRequestURI());
            };
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            LlmProperties properties = new LlmProperties();
            properties.getChat().setBaseUrl(baseUrl);
            properties.getChat().getOptions().setModel("chat-model");
            properties.getEmbedding().setBaseUrl(baseUrl);
            properties.getEmbedding().getOptions().setModel("embedding-model");
            properties.getEmbedding().getOptions().setDimensions(2);
            LlmConfig config = new LlmConfig(properties);

            assertThat(config.llmChatModel(config.llmChatApi()).call("test")).isEqualTo("ok");
            assertThat(config.llmEmbeddingModel(config.llmEmbeddingApi()).embed("test"))
                .containsExactly(0.25f, 0.5f);
            assertThat(protocols).containsExactly("HTTP/1.1", "HTTP/1.1");
            assertThat(upgrades).containsExactly("", "");
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("reasoning-effort를 정하면 두 채팅 모델 요청에 싣고, 비우면 보내지 않는다")
    void reasoningEffort_isSentOnlyWhenConfigured() throws Exception {
        var bodies = new ConcurrentLinkedQueue<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                {"id":"test","object":"chat.completion","created":1,"model":"chat-model",
                 "choices":[{"index":0,"message":{"role":"assistant","content":"ok"},"finish_reason":"stop"}],
                 "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            LlmProperties properties = new LlmProperties();
            properties.getChat().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.getChat().getOptions().setModel("chat-model");
            LlmConfig config = new LlmConfig(properties);

            config.llmChatModel(config.llmChatApi()).call("test");
            properties.getChat().getOptions().setReasoningEffort("none");
            config.llmChatModel(config.llmChatApi()).call("test");
            config.llmPublishingChatModel(config.llmPublishingApi()).call("test");

            assertThat(bodies).hasSize(3);
            assertThat(bodies.poll()).doesNotContain("reasoning_effort");
            assertThat(bodies).allSatisfy(body -> assertThat(body).contains("\"reasoning_effort\":\"none\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("LLM 설정은 chat과 embedding용 compatible client를 분리해 만든다")
    void llmConfig_buildsSeparateChatAndEmbeddingModels() {
        LlmProperties properties = new LlmProperties();
        properties.getChat().setBaseUrl("http://chat.local");
        properties.getChat().getOptions().setModel("chat-model");
        properties.getEmbedding().setBaseUrl("http://embedding.local");
        properties.getEmbedding().getOptions().setModel("embedding-model");
        properties.getEmbedding().getOptions().setDimensions(1024);
        LlmConfig config = new LlmConfig(properties);

        OpenAiApi chatApi = config.llmChatApi();
        OpenAiApi embeddingApi = config.llmEmbeddingApi();
        OpenAiChatModel chatModel = config.llmChatModel(chatApi);
        OpenAiEmbeddingModel embeddingModel = config.llmEmbeddingModel(embeddingApi);

        assertThat(chatApi).isNotSameAs(embeddingApi);
        assertThat(chatModel.getDefaultOptions().getModel()).isEqualTo("chat-model");
        assertThat(embeddingModel).isNotNull();
    }

    @Test
    @DisplayName("발행 생성은 게이트웨이 503을 재시도하지 않는다")
    void publishingModel_doesNotRetryGatewayFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            calls.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"detail\":\"llm gateway busy\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(503, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            LlmProperties properties = new LlmProperties();
            properties.getChat().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.getChat().getOptions().setModel("chat-model");
            LlmConfig config = new LlmConfig(properties);

            assertThatThrownBy(() -> config.llmPublishingChatModel(config.llmPublishingApi()).call("test"))
                .hasMessageContaining("503");
            assertThat(calls).hasValue(1);
        } finally {
            server.stop(0);
        }
    }
}
