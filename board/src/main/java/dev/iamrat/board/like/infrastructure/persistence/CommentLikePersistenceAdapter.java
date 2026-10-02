package dev.iamrat.board.like.infrastructure.persistence;

import dev.iamrat.board.like.application.CommentLikeStore;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentLikePersistenceAdapter implements CommentLikeStore {

    private final CommentLikeRepository commentLikeRepository;
    private final Clock clock;
    private final AuditorAware<String> auditorAware;

    @Override
    public boolean existsByCommentIdAndAccountId(Long commentId, Long accountId) {
        return commentLikeRepository.existsByComment_IdAndAccountId(commentId, accountId);
    }

    @Override
    public boolean insertIfAbsent(Long commentId, Long accountId) {
        return commentLikeRepository.insertIfAbsent(commentId, accountId, LocalDateTime.now(clock), auditorAware.getCurrentAuditor().orElseThrow()) == 1;
    }

    @Override
    public long countByCommentId(Long commentId) {
        return commentLikeRepository.countByComment_Id(commentId);
    }

    @Override
    public long deleteByCommentIdAndAccountId(Long commentId, Long accountId) {
        return commentLikeRepository.deleteByCommentIdAndAccountId(commentId, accountId);
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
