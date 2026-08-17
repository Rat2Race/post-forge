package dev.iamrat.source.infrastructure.naver;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NaverSearchMetrics {

    private static final String FETCH_TIMER = "external_naver_fetch";
    private static final String SUCCESS_COUNTER = "external_naver_fetch_success_total";
    private static final String FAILURE_COUNTER = "external_naver_fetch_failure_total";
    private static final String ITEMS_COUNTER = "external_naver_fetch_items_total";
    private static final List<String> APIS = List.of("news", "shopping");

    private final MeterRegistry meterRegistry;

    public NaverSearchMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        APIS.forEach(this::registerIdleMeters);
    }

    public Observation start(String api) {
        return new Observation(api, Timer.start(meterRegistry));
    }

    public void recordSuccess(String api, int itemCount) {
        counter(SUCCESS_COUNTER, api).increment();
        counter(ITEMS_COUNTER, api).increment(itemCount);
    }

    public void recordFailure(String api, RuntimeException exception) {
        Counter.builder(FAILURE_COUNTER)
            .tag("api", api)
            .tag("exception", exception.getClass().getSimpleName())
            .tag("status", status(exception))
            .register(meterRegistry)
            .increment();
    }

    private Counter counter(String name, String api) {
        return Counter.builder(name)
            .tag("api", api)
            .register(meterRegistry);
    }

    private void registerIdleMeters(String api) {
        counter(SUCCESS_COUNTER, api);
        counter(ITEMS_COUNTER, api);
        Timer.builder(FETCH_TIMER)
            .tag("api", api)
            .tag("outcome", "success")
            .tag("status", "200")
            .register(meterRegistry);
    }

    private String status(RuntimeException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return String.valueOf(responseException.getStatusCode().value());
        }
        return "none";
    }

    public class Observation {

        private final String api;
        private final Timer.Sample sample;

        private Observation(String api, Timer.Sample sample) {
            this.api = api;
            this.sample = sample;
        }

        public void stopSuccess() {
            stop("success", "200");
        }

        public void stopFailure(RuntimeException exception) {
            stop("failure", status(exception));
        }

        private void stop(String outcome, String status) {
            sample.stop(Timer.builder(FETCH_TIMER)
                .tag("api", api)
                .tag("outcome", outcome)
                .tag("status", status)
                .register(meterRegistry));
        }
    }
}
