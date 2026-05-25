package dev.iamrat.board.like.infrastructure.persistence;

import dev.iamrat.board.like.application.PostLikeStore;
import dev.iamrat.board.like.domain.PostLike;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostLikePersistenceAdapter implements PostLikeStore {

    private final PostLikeRepository postLikeRepository;

    @Override
    public boolean existsByPostIdAndAccountId(Long postId, Long accountId) {
        return postLikeRepository.existsByPost_IdAndAccountId(postId, accountId);
    }

    @Override
    public PostLike save(PostLike postLike) {
        return postLikeRepository.save(postLike);
    }

    @Override
    public long countByPostId(Long postId) {
        return postLikeRepository.countByPost_Id(postId);
    }

    @Override
    public long deleteByPostIdAndAccountId(Long postId, Long accountId) {
        return postLikeRepository.deleteByPost_IdAndAccountId(postId, accountId);
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
