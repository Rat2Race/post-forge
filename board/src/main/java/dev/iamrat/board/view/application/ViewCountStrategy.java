package dev.iamrat.board.view.application;

import java.util.List;
import java.util.Map;

public interface ViewCountStrategy {

    void incrementIfNew(Long postId, Long accountId);

    long getViewCount(Long postId);

    Map<Long, Long> getViewCounts(List<Long> postIds);

    void deleteViewCount(Long postId);
}
