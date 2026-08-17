package dev.iamrat.board.purchase.infrastructure.persistence;

import dev.iamrat.board.purchase.application.PurchaseVoteCount;
import dev.iamrat.board.purchase.application.PurchaseVoteStore;
import dev.iamrat.board.purchase.domain.PostPurchaseVote;
import dev.iamrat.board.purchase.domain.PurchaseVoteType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PurchaseVotePersistenceAdapter implements PurchaseVoteStore {

    private final PurchaseVoteRepository purchaseVoteRepository;

    @Override
    public Optional<PostPurchaseVote> findByPostIdAndAccountId(Long postId, Long accountId) {
        return purchaseVoteRepository.findByPost_IdAndAccountId(postId, accountId);
    }

    @Override
    public List<PostPurchaseVote> findByAccountIdAndPostIds(Long accountId, List<Long> postIds) {
        return purchaseVoteRepository.findByAccountIdAndPost_IdIn(accountId, postIds);
    }

    @Override
    public PostPurchaseVote save(PostPurchaseVote vote) {
        return purchaseVoteRepository.save(vote);
    }

    @Override
    public long deleteByPostIdAndAccountId(Long postId, Long accountId) {
        return purchaseVoteRepository.deleteByPost_IdAndAccountId(postId, accountId);
    }

    @Override
    public List<PurchaseVoteCount> countByPostIds(List<Long> postIds) {
        return purchaseVoteRepository.countByPostIds(postIds).stream()
            .map(row -> new PurchaseVoteCount(
                (Long) row[0],
                (PurchaseVoteType) row[1],
                (Long) row[2]
            ))
            .toList();
    }
}
