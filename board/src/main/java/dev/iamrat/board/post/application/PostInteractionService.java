package dev.iamrat.board.post.application;

import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.board.like.application.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostInteractionService {

    private final PostReader postReader;
    private final PostLikeService postLikeService;

    @Transactional
    public LikeResult likePost(Long postId, Long accountId) {
        postReader.requireExists(postId);
        return postLikeService.like(postId, accountId);
    }

    @Transactional
    public LikeResult unlikePost(Long postId, Long accountId) {
        postReader.requireExists(postId);
        return postLikeService.unlike(postId, accountId);
    }
}
