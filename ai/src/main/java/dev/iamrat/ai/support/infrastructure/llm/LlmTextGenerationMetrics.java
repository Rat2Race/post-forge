package dev.iamrat.ai.support.infrastructure.llm;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmTextGenerationMetrics {

    private final MeterRegistry meterRegistry;
    private final LlmProperties llmProperties;

    public Observation start(String systemPrompt, String userPrompt) {
        recordSummary("ai_text_generation_prompt_chars", systemPrompt.length() + userPrompt.length());
        return new Observation(Timer.start(meterRegistry));
    }

    public void recordSuccess(ChatResponse chatResponse, String response) {
        recordUsage(chatResponse);
        recordSummary("ai_text_generation_response_chars", response == null ? 0 : response.length());
        counter("ai_text_generation_success_total").increment();
    }

    public void recordDegraded() {
        counter("ai_text_generation_degraded_total").increment();
    }

    public String provider() {
        return llmProperties.getProvider();
    }

    private void recordUsage(ChatResponse chatResponse) {
        Usage usage = chatResponse.getMetadata().getUsage();

        if (usage.getPromptTokens() == 0
                && usage.getCompletionTokens() == 0
                && usage.getTotalTokens() == 0) {
            log.debug("AI token usage 메타데이터가 제공되지 않아 token metric 기록을 건너뜁니다.");
            return;
        }

        recordSummary("ai_text_generation_prompt_tokens", usage.getPromptTokens());
        recordSummary("ai_text_generation_completion_tokens", usage.getCompletionTokens());
        recordSummary("ai_text_generation_total_tokens", usage.getTotalTokens());
    }

    private void recordSummary(String name, Number value) {
        if (value == null) {
            return;
        }
        DistributionSummary.builder(name)
            .tag("provider", provider())
            .register(meterRegistry)
            .record(value.doubleValue());
    }

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

        public void stop() {
            sample.stop(Timer.builder("ai_text_generation")
                .tag("provider", provider())
                .register(meterRegistry));
        }
    }
}
