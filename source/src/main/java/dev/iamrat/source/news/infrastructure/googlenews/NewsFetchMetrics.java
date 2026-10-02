package dev.iamrat.source.news.infrastructure.googlenews;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NewsFetchMetrics {

    private static final String FETCH_TIMER = "external_news_fetch";
    private static final String SUCCESS_COUNTER = "external_news_fetch_success_total";
    private static final String FAILURE_COUNTER = "external_news_fetch_failure_total";
    private static final String ITEMS_COUNTER = "external_news_fetch_items_total";
    private static final String API = "news";

    private final MeterRegistry meterRegistry;

    public NewsFetchMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        counter(SUCCESS_COUNTER);
        counter(ITEMS_COUNTER);
        fetchTimer("success", "200");
    }

    public Observation start() {
        return new Observation(Timer.start(meterRegistry));
    }

    public void recordSuccess(int itemCount) {
        counter(SUCCESS_COUNTER).increment();
        counter(ITEMS_COUNTER).increment(itemCount);
    }

    public void recordFailure(RuntimeException exception) {
        Counter.builder(FAILURE_COUNTER)
            .tag("api", API)
            .tag("exception", exception.getClass().getSimpleName())
            .tag("status", status(exception))
            .register(meterRegistry)
            .increment();
    }

    static String status(RuntimeException exception) {
        return exception instanceof RestClientResponseException response
            ? String.valueOf(response.getStatusCode().value())
            : "none";
    }

    private Counter counter(String name) {
        return Counter.builder(name).tag("api", API).register(meterRegistry);
    }

    private Timer fetchTimer(String outcome, String status) {
        return Timer.builder(FETCH_TIMER)
            .tag("api", API)
            .tag("outcome", outcome)
            .tag("status", status)
            .register(meterRegistry);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public class Observation {

        private final Timer.Sample sample;

        public void stopSuccess() {
            sample.stop(fetchTimer("success", "200"));
        }

        public void stopFailure(RuntimeException exception) {
            sample.stop(fetchTimer("failure", status(exception)));
        }
    }
}
