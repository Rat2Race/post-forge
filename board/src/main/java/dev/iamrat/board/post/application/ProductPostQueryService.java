package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.presentation.dto.PostDetailResponse;
import dev.iamrat.board.post.infrastructure.persistence.PostProductLinkRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPostQueryService {

    private final PostProductLinkRepository postProductLinkRepository;

    public List<PostDetailResponse> getProductPosts(Long productId) {
        return postProductLinkRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
            .map(link -> toResponse(link.getPost()))
            .toList();
    }

    private PostDetailResponse toResponse(Post post) {
        return PostDetailResponse.from(post, false, post.getLikeCount(), post.getComments().size(), post.getViews());
    }
}
