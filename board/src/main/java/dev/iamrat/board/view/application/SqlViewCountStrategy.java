package dev.iamrat.board.view.application;

import dev.iamrat.board.post.application.PostViewCountService;
import io.micrometer.core.instrument.Timer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "postforge.view-count", name = "mode", havingValue = "sql")
public class SqlViewCountStrategy implements ViewCountStrategy {

    private final PostViewCountService postViewCountService;
    private final ViewCountMetrics viewCountMetrics;

    @Override
    public void incrementIfNew(Long postId, Long accountId) {
        Timer.Sample sample = viewCountMetrics.start();
        String durationResult = ViewCountMetrics.RESULT_FAILURE;
        try {
            postViewCountService.incrementViewCount(postId);
            viewCountMetrics.recordOperation(ViewCountMetrics.OPERATION_INCREMENT, ViewCountMetrics.RESULT_SUCCESS);
            durationResult = ViewCountMetrics.RESULT_SUCCESS;
        } catch (RuntimeException e) {
            viewCountMetrics.recordOperation(ViewCountMetrics.OPERATION_INCREMENT, ViewCountMetrics.RESULT_FAILURE);
            throw e;
        } finally {
            viewCountMetrics.recordDuration(sample, ViewCountMetrics.OPERATION_INCREMENT, durationResult);
        }
    }

    @Override
    public long getViewCount(Long postId) {
        Timer.Sample sample = viewCountMetrics.start();
        String durationResult = ViewCountMetrics.RESULT_FAILURE;
        try {
            long viewCount = postViewCountService.getViewCount(postId);
            viewCountMetrics.recordDbLoad(ViewCountMetrics.OPERATION_GET, ViewCountMetrics.RESULT_SUCCESS);
            durationResult = ViewCountMetrics.RESULT_SUCCESS;
            return viewCount;
        } catch (RuntimeException e) {
            viewCountMetrics.recordDbLoad(ViewCountMetrics.OPERATION_GET, ViewCountMetrics.RESULT_FAILURE);
            throw e;
        } finally {
            viewCountMetrics.recordDuration(sample, ViewCountMetrics.OPERATION_GET, durationResult);
        }
    }

    @Override
    public Map<Long, Long> getViewCounts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Timer.Sample sample = viewCountMetrics.start();
        String durationResult = ViewCountMetrics.RESULT_FAILURE;
        try {
            Map<Long, Long> viewCounts = postViewCountService.findViewCounts(postIds);
            viewCountMetrics.recordDbLoad(
                ViewCountMetrics.OPERATION_GET_MANY,
                ViewCountMetrics.RESULT_SUCCESS,
                postIds.size()
            );
            durationResult = ViewCountMetrics.RESULT_SUCCESS;
            return viewCounts;
        } catch (RuntimeException e) {
            viewCountMetrics.recordDbLoad(
                ViewCountMetrics.OPERATION_GET_MANY,
                ViewCountMetrics.RESULT_FAILURE,
                postIds.size()
            );
            throw e;
        } finally {
            viewCountMetrics.recordDuration(sample, ViewCountMetrics.OPERATION_GET_MANY, durationResult);
        }
    }

    @Override
    public void deleteViewCount(Long postId) {
        Timer.Sample sample = viewCountMetrics.start();
        try {
            viewCountMetrics.recordOperation(ViewCountMetrics.OPERATION_DELETE, ViewCountMetrics.RESULT_SKIPPED);
        } finally {
            viewCountMetrics.recordDuration(sample, ViewCountMetrics.OPERATION_DELETE, ViewCountMetrics.RESULT_SKIPPED);
        }
    }
}
