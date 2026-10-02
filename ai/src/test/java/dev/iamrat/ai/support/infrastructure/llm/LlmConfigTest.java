package dev.iamrat.ai.support.infrastructure.llm;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.retry.TransientAiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class LlmConfigTest {

    @Test
    @DisplayName("채팅 요청은 HTTP/1.1로 보내고 h2c 업그레이드를 요청하지 않는다")
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
            LlmConfig config = new LlmConfig(properties);

            assertThat(config.llmChatModel(config.llmChatApi()).call("test")).isEqualTo("ok");
            assertThat(protocols).containsExactly("HTTP/1.1");
            assertThat(upgrades).containsExactly("");
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("reasoning-effort를 정하면 채팅 요청에 싣고, 비우면 보내지 않는다")
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

            assertThat(bodies).hasSize(2);
            assertThat(bodies.poll()).doesNotContain("reasoning_effort");
            assertThat(bodies).allSatisfy(body -> assertThat(body).contains("\"reasoning_effort\":\"none\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("게이트웨이가 503을 돌려주면 모델이 스스로 다시 보내지 않고 한 번만 보낸 뒤 바로 실패한다")
    void gatewayFailure_isNotRetriedByModel() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            hits.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            byte[] response = "{\"detail\":\"llm gateway busy\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(503, response.length);
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
            OpenAiChatModel chatModel = config.llmChatModel(config.llmChatApi());

            // Spring AI 기본 RetryTemplate은 2초부터 간격을 늘려 10번까지 다시 보낸다. 5초 안에 끝나지 않으면 재시도가 켜져 있다.
            assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThatThrownBy(() -> chatModel.call("test")).isInstanceOf(TransientAiException.class));
            assertThat(hits).hasValue(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("LLM 설정은 지정한 chat 모델로 compatible client를 만든다")
    void llmConfig_buildsChatModelWithConfiguredModel() {
        LlmProperties properties = new LlmProperties();
        properties.getChat().setBaseUrl("http://chat.local");
        properties.getChat().getOptions().setModel("chat-model");
        LlmConfig config = new LlmConfig(properties);

        OpenAiChatModel chatModel = config.llmChatModel(config.llmChatApi());

        assertThat(chatModel.getDefaultOptions().getModel()).isEqualTo("chat-model");
    }
}
