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
import static org.mockito.Mockito.clearInvocations;
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
        clearInvocations(chatModel);

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

        // provider별 성공 호출 수: 정상 응답 비율과 장애 전환 여부를 확인한다.
        assertThat(meterRegistry.counter("ai_text_generation_success_total", "provider", "ollama").count())
            .isEqualTo(1.0);

        // provider별 전체 처리 시간: 성공/실패와 관계없이 LLM 호출 지연을 추적한다.
        assertThat(meterRegistry.find("ai_text_generation")
            .tag("provider", "ollama")
            .timer()
            .count()).isEqualTo(1);

        // completion token 수: 모델이 응답을 생성하는 데 사용한 출력 토큰량을 확인한다.
        assertThat(meterRegistry.find("ai_text_generation_completion_tokens")
            .tag("provider", "ollama")
            .summary()
            .totalAmount()).isEqualTo(3.0);

        // prompt token 수: 모델에 전달한 입력 프롬프트의 토큰량을 확인한다.
        assertThat(meterRegistry.find("ai_text_generation_prompt_tokens")
            .tag("provider", "ollama")
            .summary()
            .totalAmount()).isEqualTo(10.0);

        // total token 수: prompt와 completion을 합친 전체 토큰 사용량을 확인한다.
        assertThat(meterRegistry.find("ai_text_generation_total_tokens")
            .tag("provider", "ollama")
            .summary()
            .totalAmount()).isEqualTo(13.0);

        // prompt 문자 수: token usage가 없어도 요청 입력 크기를 추적할 수 있게 한다.
        assertThat(meterRegistry.find("ai_text_generation_prompt_chars")
            .tag("provider", "ollama")
            .summary()
            .totalAmount()).isEqualTo(10.0);

        // response 문자 수: 생성된 응답의 대략적인 출력 크기를 추적한다.
        assertThat(meterRegistry.find("ai_text_generation_response_chars")
            .tag("provider", "ollama")
            .summary()
            .totalAmount()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("LLM 호출이 실패하면 대체 처리를 위해 null을 반환한다")
    void generate_whenLlmUnavailable_returnsNull() {
        LlmTextGenerationAdapter adapter = adapter("gateway");
        willThrow(new IllegalStateException("quota"))
            .given(chatModel)
            .call(any(Prompt.class));

        String response = adapter.generate("system", "user");

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("LLM 호출이 실패하면 성능저하 관측 지표를 기록한다")
    void generate_whenLlmUnavailable_recordsDegradedMetrics() {
        LlmTextGenerationAdapter adapter = adapter("gateway");
        willThrow(new IllegalStateException("quota"))
            .given(chatModel)
            .call(any(Prompt.class));

        adapter.generate("system", "user");

        // provider별 성능저하 호출 수: 예외로 fallback/null 경로를 탄 횟수를 확인한다.
        assertThat(meterRegistry.counter("ai_text_generation_degraded_total", "provider", "gateway").count())
            .isEqualTo(1.0);

        // 실패한 호출도 timer에 포함해 전체 LLM 호출 지연과 실패 비용을 함께 본다.
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
