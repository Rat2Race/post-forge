package dev.iamrat.board.post.presentation;

import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.board.like.presentation.dto.LikeResponse;
import dev.iamrat.board.post.application.PostCommandService;
import dev.iamrat.board.post.application.PostInteractionService;
import dev.iamrat.board.post.application.PostQueryService;
import dev.iamrat.board.post.presentation.dto.PostDetailResponse;
import dev.iamrat.board.post.presentation.dto.PostRequest;
import dev.iamrat.board.post.presentation.dto.PostSummaryResponse;
import dev.iamrat.core.global.dto.MessageResponse;
import dev.iamrat.core.global.dto.PageResponse;
import dev.iamrat.core.account.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/posts", "/api/posts"})
public class PostController {

    private final PostCommandService postCommandService;
    private final PostQueryService postQueryService;
    private final PostInteractionService postInteractionService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostSummaryResponse> createPost(
        @RequestBody @Valid PostRequest postRequest,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        PostSummaryResponse savedPost = postCommandService.savePost(
            postRequest.title(),
            postRequest.content(),
            postRequest.summary(),
            postRequest.tags(),
            postRequest.category(),
            accountId(user)
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(savedPost);
    }

    @GetMapping
    public ResponseEntity<PageResponse<PostDetailResponse>> getPosts(
        @RequestParam(required = false) String keyword,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        Long accountId = optionalAccountId(user);

        Page<PostDetailResponse> posts = keyword != null
            ? postQueryService.searchPosts(keyword, pageable, accountId)
            : postQueryService.getPosts(pageable, accountId);

        return ResponseEntity.ok(PageResponse.from(posts));
    }

    @GetMapping("/{postId:\\d+}")
    public ResponseEntity<PostDetailResponse> getPost(
        @PathVariable("postId") Long postId,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        Long accountId = optionalAccountId(user);
        PostDetailResponse post = postQueryService.readPost(postId, accountId);

        return ResponseEntity.ok(post);
    }

    @PutMapping("/{postId:\\d+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @postCommandService.isOwner(#postId, principal.accountId)")
    public ResponseEntity<PostSummaryResponse> updatePost(
        @PathVariable("postId") Long postId,
        @RequestBody @Valid PostRequest postRequest
    ) {
        PostSummaryResponse modifiedPost = postCommandService.updatePost(
            postId,
            postRequest.title(),
            postRequest.content(),
            postRequest.summary(),
            postRequest.tags(),
            postRequest.category()
        );

        return ResponseEntity.ok(modifiedPost);
    }

    @DeleteMapping("/{postId:\\d+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @postCommandService.isOwner(#postId, principal.accountId)")
    public ResponseEntity<MessageResponse> deletePost(
        @PathVariable("postId") Long postId
    ) {
        postCommandService.deletePost(postId);

        return ResponseEntity.ok(MessageResponse.of("게시글 삭제 완료"));
    }

    @PostMapping("/{postId:\\d+}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> likePost(
        @PathVariable("postId") Long postId,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        LikeResult likeStatus = postInteractionService.likePost(postId, accountId(user));

        return ResponseEntity.ok(LikeResponse.from(likeStatus));
    }

    @DeleteMapping("/{postId:\\d+}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> unlikePost(
        @PathVariable("postId") Long postId,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        LikeResult likeStatus = postInteractionService.unlikePost(postId, accountId(user));

        return ResponseEntity.ok(LikeResponse.from(likeStatus));
    }

    private static Long optionalAccountId(UserPrincipal user) {
        return user != null ? accountId(user) : null;
    }

    private static Long accountId(UserPrincipal user) {
        return user.getAccountId();
    }
}
