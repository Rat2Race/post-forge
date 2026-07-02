package dev.iamrat.board.purchase.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.purchase.domain.PostPurchaseVote;
import dev.iamrat.board.purchase.domain.PurchaseVoteType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseVoteQueryService {

    private final PurchaseVoteStore purchaseVoteStore;
    private final PurchaseVoteEligibility purchaseVoteEligibility;

    public PurchaseVoteSummary getSummary(Post post, Long accountId) {
        return getSummaries(List.of(post), accountId).get(post.getId());
    }

    public Map<Long, PurchaseVoteSummary> getSummaries(List<Post> posts, Long accountId) {
        if (posts == null || posts.isEmpty()) {
            return Map.of();
        }

        List<Post> eligiblePosts = posts.stream()
            .filter(purchaseVoteEligibility::isEligible)
            .toList();
        List<Long> eligiblePostIds = eligiblePosts.stream()
            .map(Post::getId)
            .toList();

        Map<Long, EnumMap<PurchaseVoteType, Long>> countMap = countMap(eligiblePostIds);
        Map<Long, PurchaseVoteType> myVoteMap = myVoteMap(accountId, eligiblePostIds);

        Map<Long, PurchaseVoteSummary> summaries = new HashMap<>();
        for (Post post : posts) {
            boolean eligible = purchaseVoteEligibility.isEligible(post);
            EnumMap<PurchaseVoteType, Long> counts = countMap.getOrDefault(post.getId(), emptyCounts());
            summaries.put(post.getId(), new PurchaseVoteSummary(
                post.getId(),
                eligible,
                counts.get(PurchaseVoteType.BUYABLE),
                counts.get(PurchaseVoteType.UNSURE),
                counts.get(PurchaseVoteType.WAIT),
                myVoteMap.get(post.getId())
            ));
        }
        return summaries;
    }

    private Map<Long, EnumMap<PurchaseVoteType, Long>> countMap(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, EnumMap<PurchaseVoteType, Long>> result = new HashMap<>();
        for (PurchaseVoteCount count : purchaseVoteStore.countByPostIds(postIds)) {
            result.computeIfAbsent(count.postId(), ignored -> emptyCounts())
                .put(count.voteType(), count.count());
        }
        return result;
    }

    private Map<Long, PurchaseVoteType> myVoteMap(Long accountId, List<Long> postIds) {
        if (accountId == null || postIds.isEmpty()) {
            return Map.of();
        }
        return purchaseVoteStore.findByAccountIdAndPostIds(accountId, postIds).stream()
            .collect(Collectors.toMap(vote -> vote.getPost().getId(), PostPurchaseVote::getVoteType));
    }

    private EnumMap<PurchaseVoteType, Long> emptyCounts() {
        EnumMap<PurchaseVoteType, Long> counts = new EnumMap<>(PurchaseVoteType.class);
        counts.put(PurchaseVoteType.BUYABLE, 0L);
        counts.put(PurchaseVoteType.UNSURE, 0L);
        counts.put(PurchaseVoteType.WAIT, 0L);
        return counts;
    }
}
