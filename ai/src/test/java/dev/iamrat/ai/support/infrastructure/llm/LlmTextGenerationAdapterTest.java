package dev.iamrat.ai.support.infrastructure.llm;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LlmTextGenerationAdapterTest {

    @Mock
    private ChatModel chatModel;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    @DisplayName("시스템과 사용자 프롬프트를 ChatModel Prompt로 변환해 ChatModel을 호출한다")
    void generate_buildsChatModelPrompt() {
        LlmTextGenerationAdapter adapter = adapter("ollama");
        given(chatModel.call(any(Prompt.class))).willReturn(chatResponse("응답", 10, 3));

        String response = adapter.generate("system", "user");

        assertThat(response).isEqualTo("응답");

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());

        Prompt prompt = captor.getValue();

        assertThat(prompt.getInstructions()).hasSize(2);
        assertThat(prompt.getInstructions().getFirst()).isInstanceOf(SystemMessage.class);
        assertThat(((SystemMessage) prompt.getInstructions().getFirst()).getText()).isEqualTo("system");
        assertThat(prompt.getInstructions().get(1)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) prompt.getInstructions().get(1)).getText()).isEqualTo("user");
    }

    @Test
    @DisplayName("LLM 호출이 성공하면 성공 관측 지표를 기록한다")
    void generate_whenLlmSucceeds_recordsSuccessMetrics() {
        LlmTextGenerationAdapter adapter = adapter("ollama");
        given(chatModel.call(any(Prompt.class))).willReturn(chatResponse("응답", 10, 3));

        adapter.generate("system", "user");

        assertThat(meterRegistry.counter("ai_text_generation_success_total", "provider", "ollama").count())
            .isEqualTo(1.0);
        assertThat(meterRegistry.find("ai_text_generation")
            .tag("provider", "ollama")
            .timer()
            .count()).isEqualTo(1);
        assertThat(meterRegistry.find("ai_text_generation_prompt_tokens")
            .tag("provider", "ollama")
            .summary()
            .totalAmount()).isEqualTo(10.0);
        assertThat(meterRegistry.find("ai_text_generation_completion_tokens")
            .tag("provider", "ollama")
            .summary()
            .totalAmount()).isEqualTo(3.0);
        assertThat(meterRegistry.find("ai_text_generation_prompt_chars")
            .tag("provider", "ollama")
            .summary()
            .totalAmount()).isEqualTo(10.0);
        assertThat(meterRegistry.find("ai_text_generation_response_chars")
            .tag("provider", "ollama")
            .summary()
            .totalAmount()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("LLM 호출이 실패하면 null을 반환하고 성능저하 지표를 기록한다")
    void generate_whenLlmUnavailable_returnsNullAndRecordsDegradedMetrics() {
        LlmTextGenerationAdapter adapter = adapter("gateway");
        willThrow(new IllegalStateException("quota"))
            .given(chatModel)
            .call(any(Prompt.class));

        String response = adapter.generate("system", "user");

        assertThat(response).isNull();
        assertThat(meterRegistry.counter("ai_text_generation_degraded_total", "provider", "gateway").count())
            .isEqualTo(1.0);
        assertThat(meterRegistry.find("ai_text_generation")
            .tag("provider", "gateway")
            .timer()
            .count()).isEqualTo(1);
    }

    private LlmTextGenerationAdapter adapter(String provider) {
        return new LlmTextGenerationAdapter(
            chatModel,
            metrics(provider)
        );
    }

    private LlmTextGenerationMetrics metrics(String provider) {
        return new LlmTextGenerationMetrics(meterRegistry, llmProperties(provider));
    }

    private LlmProperties llmProperties(String provider) {
        LlmProperties properties = new LlmProperties();
        properties.setProvider(provider);
        return properties;
    }

    private ChatResponse chatResponse(String text, int promptTokens, int completionTokens) {
        return new ChatResponse(
            List.of(new Generation(new AssistantMessage(text))),
            ChatResponseMetadata.builder()
                .usage(new DefaultUsage(promptTokens, completionTokens))
                .build()
        );
    }
}
