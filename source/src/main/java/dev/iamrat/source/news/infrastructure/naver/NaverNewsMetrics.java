package dev.iamrat.source.news.infrastructure.naver;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NaverNewsMetrics {

    private static final String FETCH_TIMER = "external_naver_fetch";
    private static final String SUCCESS_COUNTER = "external_naver_fetch_success_total";
    private static final String FAILURE_COUNTER = "external_naver_fetch_failure_total";
    private static final String ITEMS_COUNTER = "external_naver_fetch_items_total";
    private static final String API = "news";

    private final MeterRegistry meterRegistry;

    public NaverNewsMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        registerIdleMeters();
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
        if (exception instanceof RestClientResponseException responseException) {
            return String.valueOf(responseException.getStatusCode().value());
        }
        return "none";
    }

    private Counter counter(String name) {
        return Counter.builder(name)
            .tag("api", API)
            .register(meterRegistry);
    }

    // 첫 increment 전까지는 시계열이 스크레이프에 안 잡혀 대시보드가 "no data"가 되므로 기동 시 미리 등록한다.
    private void registerIdleMeters() {
        counter(SUCCESS_COUNTER);
        counter(ITEMS_COUNTER);
        fetchTimer("success", "200");
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
