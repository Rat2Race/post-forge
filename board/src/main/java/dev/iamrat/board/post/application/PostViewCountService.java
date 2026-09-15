package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostViewCountService {
    private final PostStore postStore;

    public long getViewCount(Long postId) {
        if (postId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }

        return postStore.findById(postId)
            .map(Post::getViews)
            .orElseThrow(() -> new CustomException(BoardErrorCode.POST_NOT_FOUND));
    }

    public Map<Long, Long> findViewCounts(List<Long> postIds) {
        if (postIds == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }

        Map<Long, Long> result = new HashMap<>();
        postStore.findAllById(postIds).forEach(post -> result.put(post.getId(), post.getViews()));

        return result;
    }

    @Transactional
    public void updateViewCount(Long postId, long views) {
        if (postId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }

        postStore.updateViews(postId, views);
    }
}
