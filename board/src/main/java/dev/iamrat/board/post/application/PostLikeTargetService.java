package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeTargetService {

    private final PostStore postStore;

    public Post getReference(Long postId) {
        if (postId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
        return postStore.getReferenceById(postId);
    }

    @Transactional
    public void updateLikeCount(Long postId, long likeCount) {
        if (postId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
        postStore.updateLikeCount(postId, likeCount);
    }
}
