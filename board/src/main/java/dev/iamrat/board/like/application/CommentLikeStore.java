package dev.iamrat.board.like.application;

import java.util.List;
import java.util.Set;

public interface CommentLikeStore {

    boolean existsByCommentIdAndAccountId(Long commentId, Long accountId);

    boolean insertIfAbsent(Long commentId, Long accountId);

    long countByCommentId(Long commentId);

    long deleteByCommentIdAndAccountId(Long commentId, Long accountId);

    List<Object[]> countByCommentIds(List<Long> commentIds);

    Set<Long> findLikedCommentIdsByAccountIdAndCommentIds(Long accountId, List<Long> commentIds);
}
