package dev.iamrat.board.like.application;

import java.util.List;
import java.util.Set;

public interface PostLikeStore {

    boolean existsByPostIdAndAccountId(Long postId, Long accountId);

    boolean insertIfAbsent(Long postId, Long accountId);

    long countByPostId(Long postId);

    long deleteByPostIdAndAccountId(Long postId, Long accountId);

    List<Object[]> countByPostIds(List<Long> postIds);

    Set<Long> findLikedPostIdsByAccountIdAndPostIds(Long accountId, List<Long> postIds);
}
