package dev.iamrat.ai.support.infrastructure.openai;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

/*
 * OpenAI 호환 텍스트 생성 호출에서 발생하는 관측 지표를 기록하는 컴포넌트입니다.
 *
 * OpenAiTextGenerationAdapter는 프롬프트를 만들고 ChatModel을 호출하는 책임만 가지도록 두고,
 * Micrometer 기반의 timer, counter, summary, token usage 기록은 이 클래스에 모읍니다.
 * 이렇게 분리하면 LLM 호출 흐름과 운영 관측 코드가 섞이지 않아 adapter의 책임이 더 선명해집니다.
 *
 * 현재 기록하는 지표:
 * - 텍스트 생성 요청 처리 시간
 * - 성공/성능저하(degraded) 호출 횟수
 * - 프롬프트/응답 문자 수
 * - Spring AI가 제공하는 prompt/completion/total token 사용량
 */
@Component
@RequiredArgsConstructor
public class OpenAiTextGenerationMetrics {

    private final MeterRegistry meterRegistry;
    private final OpenAiProperties openAiProperties;

    /*
     * 외부 LLM 호출이 시작되기 전에 요청 관측을 시작합니다.
     *
     * 프롬프트 문자 수는 호출 성공/실패와 무관하게 요청 입력 크기를 보여주는 값이므로
     * 모델 호출 전에 먼저 기록합니다. 반환된 Observation은 호출 결과와 관계없이
     * finally 블록에서 반드시 종료되어야 timer가 누락되지 않습니다.
     */
    public Observation start(String systemPrompt, String userPrompt) {
        recordSummary("ai_text_generation_prompt_chars", systemPrompt.length() + userPrompt.length());
        return new Observation(Timer.start(meterRegistry));
    }

    /*
     * LLM 호출이 정상 응답을 반환했을 때 성공 지표를 기록합니다.
     *
     * 응답 문자 수는 항상 기록하고, token usage는 Spring AI 응답 메타데이터에 존재하는 경우에만 기록합니다.
     * 로컬 모델이나 일부 OpenAI 호환 서버는 token usage를 제공하지 않을 수 있으므로 해당 값은 선택적으로 다룹니다.
     */
    public void recordSuccess(ChatResponse chatResponse, String response) {
        recordUsage(chatResponse);
        recordSummary("ai_text_generation_response_chars", response == null ? 0 : response.length());
        counter("ai_text_generation_success_total").increment();
    }

    /*
     * 외부 LLM 호출이 실패했거나 사용할 수 없는 상태일 때 성능저하 지표를 기록합니다.
     *
     * 상위 application 서비스는 실패 응답 대신 fallback 처리를 할 수 있도록 adapter에서 null을 받습니다.
     * 이 메트릭은 그런 graceful degradation 상황이 얼마나 자주 발생하는지 운영에서 확인하기 위한 값입니다.
     */
    public void recordDegraded() {
        counter("ai_text_generation_degraded_total").increment();
    }

    /*
     * 모든 텍스트 생성 메트릭에 붙이는 provider 태그 값입니다.
     *
     * 예를 들어 ollama, openai, 사내 gateway 같은 값을 넣어두면
     * 같은 지표라도 어떤 LLM 공급자 또는 호환 서버에서 발생했는지 구분할 수 있습니다.
     */
    public String provider() {
        return openAiProperties.getProvider();
    }

    /*
     * Spring AI 응답 메타데이터에 포함된 token usage를 summary 지표로 기록합니다.
     *
     * usage 메타데이터가 없는 응답은 정상적인 호환 케이스일 수 있으므로 실패로 처리하지 않고 조용히 건너뜁니다.
     */
    private void recordUsage(ChatResponse chatResponse) {
        Usage usage = chatResponse.getMetadata() == null ? null : chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return;
        }
        recordSummary("ai_text_generation_prompt_tokens", usage.getPromptTokens());
        recordSummary("ai_text_generation_completion_tokens", usage.getCompletionTokens());
        recordSummary("ai_text_generation_total_tokens", usage.getTotalTokens());
    }

    /*
     * 숫자형 값을 provider 태그가 붙은 Micrometer summary로 기록합니다.
     *
     * null 값은 기록하지 않습니다. token usage처럼 공급자별로 제공 여부가 달라질 수 있는 값을
     * 안전하게 다루기 위한 방어 코드입니다.
     */
    private void recordSummary(String name, Number value) {
        if (value == null) {
            return;
        }
        DistributionSummary.builder(name)
            .tag("provider", provider())
            .register(meterRegistry)
            .record(value.doubleValue());
    }

    /*
     * provider 태그가 붙은 counter를 생성하거나 기존 meter를 찾아 반환합니다.
     */
    private Counter counter(String name) {
        return Counter.builder(name)
            .tag("provider", provider())
            .register(meterRegistry);
    }

    public class Observation {

        private final Timer.Sample sample;

        private Observation(Timer.Sample sample) {
            this.sample = sample;
        }

        /*
         * 텍스트 생성 요청 timer를 종료합니다.
         *
         * 성공, 실패, 예외 여부와 상관없이 전체 호출 시간을 남겨야 하므로
         * adapter의 finally 블록에서 호출됩니다.
         */
        public void stop() {
            sample.stop(Timer.builder("ai_text_generation")
                .tag("provider", provider())
                .register(meterRegistry));
        }
    }
}
