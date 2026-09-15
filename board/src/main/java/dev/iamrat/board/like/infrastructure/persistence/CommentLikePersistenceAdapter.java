package dev.iamrat.board.like.infrastructure.persistence;

import dev.iamrat.board.like.application.CommentLikeStore;
import dev.iamrat.board.like.domain.CommentLike;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentLikePersistenceAdapter implements CommentLikeStore {

    private final CommentLikeRepository commentLikeRepository;

    @Override
    public boolean existsByCommentIdAndAccountId(Long commentId, Long accountId) {
        return commentLikeRepository.existsByComment_IdAndAccountId(commentId, accountId);
    }

    @Override
    public CommentLike save(CommentLike commentLike) {
        return commentLikeRepository.save(commentLike);
    }

    @Override
    public long countByCommentId(Long commentId) {
        return commentLikeRepository.countByComment_Id(commentId);
    }

    @Override
    public long deleteByCommentIdAndAccountId(Long commentId, Long accountId) {
        return commentLikeRepository.deleteByComment_IdAndAccountId(commentId, accountId);
    }

    @Override
    public List<Object[]> countByCommentIds(List<Long> commentIds) {
        return commentLikeRepository.countByCommentIds(commentIds);
    }

    @Override
    public Set<Long> findLikedCommentIdsByAccountIdAndCommentIds(Long accountId, List<Long> commentIds) {
        return commentLikeRepository.findLikedCommentIdsByAccountIdAndCommentIds(accountId, commentIds);
    }
}
