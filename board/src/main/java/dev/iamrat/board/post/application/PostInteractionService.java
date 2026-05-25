package dev.iamrat.board.post.application;

import dev.iamrat.board.like.application.LikeResponse;
import dev.iamrat.board.like.application.PostLikeService;
import dev.iamrat.board.like.application.LikeRequestGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostInteractionService {

    private final PostReader postReader;
    private final PostLikeService postLikeService;
    private final LikeRequestGuard likeRequestGuard;

    @Transactional
    public LikeResponse likePost(Long postId, Long accountId) {
        postReader.requireExists(postId);
        likeRequestGuard.guardPostLike(postId, accountId);
        return postLikeService.like(postId, accountId);
    }

    @Transactional
    public LikeResponse unlikePost(Long postId, Long accountId) {
        postReader.requireExists(postId);
        likeRequestGuard.guardPostUnlike(postId, accountId);
        return postLikeService.unlike(postId, accountId);
    }
}
