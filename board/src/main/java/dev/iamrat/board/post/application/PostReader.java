package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.PostRepository;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReader {

    private final PostRepository postRepository;

    public Post getById(Long postId) {
        if (postId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
        return postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.POST_NOT_FOUND));
    }

    public void requireExists(Long postId) {
        if (postId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }
        if (!postRepository.existsById(postId)) {
            throw new CustomException(BoardErrorCode.POST_NOT_FOUND);
        }
    }
}
