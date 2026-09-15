package dev.iamrat.board.like.application;

import dev.iamrat.board.like.domain.PostLike;
import java.util.List;
import java.util.Set;

public interface PostLikeStore {

    boolean existsByPostIdAndAccountId(Long postId, Long accountId);

    PostLike save(PostLike postLike);

    long countByPostId(Long postId);

    long deleteByPostIdAndAccountId(Long postId, Long accountId);

    List<Object[]> countByPostIds(List<Long> postIds);

    Set<Long> findLikedPostIdsByAccountIdAndPostIds(Long accountId, List<Long> postIds);
}
