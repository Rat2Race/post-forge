package dev.iamrat.board.view.application;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final ViewCountStrategy viewCountStrategy;

    public void incrementIfNew(Long postId, Long accountId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        if (accountId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);

        viewCountStrategy.incrementIfNew(postId, accountId);
    }

    public long getViewCount(Long postId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);

        return viewCountStrategy.getViewCount(postId);
    }

    public Map<Long, Long> getViewCounts(List<Long> postIds) {
        if (postIds == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return viewCountStrategy.getViewCounts(postIds);
    }

    public void deleteViewCount(Long postId) {
        if (postId == null) throw new CustomException(CommonErrorCode.INVALID_INPUT);

        viewCountStrategy.deleteViewCount(postId);
    }
}
