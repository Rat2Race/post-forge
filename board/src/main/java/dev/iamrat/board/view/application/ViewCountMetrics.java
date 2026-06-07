package dev.iamrat.board.view.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ViewCountMetrics {

    static final String ENDPOINT = "board.post.view-count";
    static final String CACHE_STATE_REDIS_ENABLED = "redis_enabled";
    static final String CACHE_STATE_SQL_ONLY = "sql_only";

    static final String CACHE_REQUESTS = "postforge_view_count_cache_requests_total";
    static final String DB_LOADS = "postforge_view_count_db_loads_total";
    static final String OPERATIONS = "postforge_view_count_operations_total";
    static final String OPERATION_DURATION = "postforge_view_count_operation_duration_seconds";

    static final String TAG_ENDPOINT = "endpoint";
    static final String TAG_CACHE_STATE = "cache_state";
    static final String TAG_OPERATION = "operation";
    static final String TAG_RESULT = "result";

    static final String OPERATION_GET = "get";
    static final String OPERATION_GET_MANY = "get_many";
    static final String OPERATION_INCREMENT = "increment";
    static final String OPERATION_LOAD_FROM_DB = "load_from_db";
    static final String OPERATION_MARK_DIRTY = "mark_dirty";
    static final String OPERATION_DELETE = "delete";

    static final String RESULT_HIT = "hit";
    static final String RESULT_MISS = "miss";
    static final String RESULT_SUCCESS = "success";
    static final String RESULT_FAILURE = "failure";
    static final String RESULT_SKIPPED = "skipped";

    private final MeterRegistry meterRegistry;
    private final ViewCountProperties viewCountProperties;

    public Timer.Sample start() {
        return Timer.start(meterRegistry);
    }

    public void recordCacheRequest(String operation, String result) {
        recordCacheRequest(operation, result, 1);
    }

    public void recordCacheRequest(String operation, String result, long amount) {
        increment(CACHE_REQUESTS, operation, result, amount);
    }

    public void recordDbLoad(String operation, String result) {
        recordDbLoad(operation, result, 1);
    }

    public void recordDbLoad(String operation, String result, long amount) {
        increment(DB_LOADS, operation, result, amount);
    }

    public void recordOperation(String operation, String result) {
        increment(OPERATIONS, operation, result, 1);
    }

    public void recordDuration(Timer.Sample sample, String operation, String result) {
        if (sample != null) {
            sample.stop(timer(operation, result));
        }
    }

    public void recordDuration(String operation, String result, Duration duration) {
        timer(operation, result).record(duration);
    }

    private void increment(String metricName, String operation, String result, long amount) {
        if (amount <= 0) {
            return;
        }
        counter(metricName, operation, result).increment(amount);
    }

    private Counter counter(String metricName, String operation, String result) {
        return Counter.builder(metricName)
            .tags(tags(operation, result))
            .register(meterRegistry);
    }

    private Timer timer(String operation, String result) {
        return Timer.builder(OPERATION_DURATION)
            .tags(tags(operation, result))
            .register(meterRegistry);
    }

    private Tags tags(String operation, String result) {
        return Tags.of(
            TAG_ENDPOINT, ENDPOINT,
            TAG_CACHE_STATE, viewCountProperties.cacheStateLabel(),
            TAG_OPERATION, operation,
            TAG_RESULT, result
        );
    }
}
