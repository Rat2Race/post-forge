package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostReferenceLink;
import dev.iamrat.board.post.presentation.dto.PostDetailResponse;
import dev.iamrat.board.post.infrastructure.persistence.PostProductLinkRepository;
import dev.iamrat.board.purchase.application.PurchaseVoteQueryService;
import dev.iamrat.board.purchase.application.PurchaseVoteSummary;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPostQueryService {

    private final PostProductLinkRepository postProductLinkRepository;
    private final PurchaseVoteQueryService purchaseVoteQueryService;
    private final PostReferenceLinkStore postReferenceLinkStore;

    public List<PostDetailResponse> getProductPosts(Long productId) {
        List<Post> posts = postProductLinkRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
            .map(link -> link.getPost())
            .toList();
        Map<Long, PurchaseVoteSummary> purchaseVotes = purchaseVoteQueryService.getSummaries(posts, null);
        List<Long> postIds = posts.stream()
            .map(Post::getId)
            .toList();
        Map<Long, List<PostReferenceLink>> references = postReferenceLinkStore.findByPostIds(postIds).stream()
            .collect(Collectors.groupingBy(reference -> reference.getPost().getId()));
        return posts.stream()
            .map(post -> toResponse(
                post,
                purchaseVotes.get(post.getId()),
                references.getOrDefault(post.getId(), List.of())
            ))
            .toList();
    }

    private PostDetailResponse toResponse(
        Post post,
        PurchaseVoteSummary purchaseVote,
        List<PostReferenceLink> references
    ) {
        return PostDetailResponse.from(
            post,
            false,
            post.getLikeCount(),
            post.getComments().size(),
            post.getViews(),
            purchaseVote,
            references
        );
    }
}
