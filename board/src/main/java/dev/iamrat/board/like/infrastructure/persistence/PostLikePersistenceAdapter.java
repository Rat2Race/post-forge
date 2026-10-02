package dev.iamrat.board.like.infrastructure.persistence;

import dev.iamrat.board.like.application.PostLikeStore;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostLikePersistenceAdapter implements PostLikeStore {

    private final PostLikeRepository postLikeRepository;
    private final Clock clock;
    private final AuditorAware<String> auditorAware;

    @Override
    public boolean existsByPostIdAndAccountId(Long postId, Long accountId) {
        return postLikeRepository.existsByPost_IdAndAccountId(postId, accountId);
    }

    @Override
    public boolean insertIfAbsent(Long postId, Long accountId) {
        return postLikeRepository.insertIfAbsent(postId, accountId, LocalDateTime.now(clock), auditorAware.getCurrentAuditor().orElseThrow()) == 1;
    }

    @Override
    public long countByPostId(Long postId) {
        return postLikeRepository.countByPost_Id(postId);
    }

    @Override
    public long deleteByPostIdAndAccountId(Long postId, Long accountId) {
        return postLikeRepository.deleteByPostIdAndAccountId(postId, accountId);
    }

    @Override
    public List<Object[]> countByPostIds(List<Long> postIds) {
        return postLikeRepository.countByPostIds(postIds);
    }

    @Override
    public Set<Long> findLikedPostIdsByAccountIdAndPostIds(Long accountId, List<Long> postIds) {
        return postLikeRepository.findLikedPostIdsByAccountIdAndPostIds(accountId, postIds);
    }
}
