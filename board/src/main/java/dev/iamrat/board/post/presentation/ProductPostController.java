package dev.iamrat.board.post.presentation;

import dev.iamrat.board.post.application.PostDetailResult;
import dev.iamrat.board.post.application.ProductPostQueryService;
import dev.iamrat.board.post.presentation.dto.PostDetailResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductPostController {

    private final ProductPostQueryService productPostQueryService;

    @GetMapping("/api/products/{productId:\\d+}/posts")
    public ResponseEntity<List<PostDetailResponse>> getProductPosts(@PathVariable Long productId) {
        List<PostDetailResult> posts = productPostQueryService.getProductPosts(productId);
        return ResponseEntity.ok(posts.stream().map(PostDetailResponse::from).toList());
    }
}
