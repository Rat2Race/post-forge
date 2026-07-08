package dev.iamrat.board.post.presentation;

import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.board.like.presentation.dto.LikeResponse;
import dev.iamrat.board.post.application.PostCommandService;
import dev.iamrat.board.post.application.PostInteractionService;
import dev.iamrat.board.post.application.PostQueryService;
import dev.iamrat.board.post.presentation.dto.PostDetailResponse;
import dev.iamrat.board.post.presentation.dto.PostRequest;
import dev.iamrat.board.post.presentation.dto.PostSummaryResponse;
import dev.iamrat.board.purchase.application.PurchaseVoteService;
import dev.iamrat.board.purchase.application.PurchaseVoteSummary;
import dev.iamrat.board.purchase.presentation.dto.PurchaseVoteRequest;
import dev.iamrat.board.purchase.presentation.dto.PurchaseVoteResponse;
import dev.iamrat.core.global.dto.MessageResponse;
import dev.iamrat.core.global.dto.PageResponse;
import dev.iamrat.core.account.UserPrincipal;
import dev.iamrat.core.board.post.PostBoardCategory;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
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
@RequestMapping("/api/posts")
public class PostController {

    private final PostCommandService postCommandService;
    private final PostQueryService postQueryService;
    private final PostInteractionService postInteractionService;
    private final PurchaseVoteService purchaseVoteService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostSummaryResponse> createPost(
        @RequestBody @Valid PostRequest postRequest,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        PostSummaryResponse savedPost = postCommandService.savePost(
            postRequest.title(),
            postRequest.content(),
            postRequest.tags(),
            postRequest.boardCategory(),
            accountId(user),
            postRequest.fileIds()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(savedPost);
    }

    @GetMapping
    public ResponseEntity<PageResponse<PostDetailResponse>> getPosts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) PostCategory category,
        @RequestParam(required = false) PostBoardCategory boardCategory,
        @RequestParam(required = false) PostPublishOrigin publishOrigin,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        Long accountId = optionalAccountId(user);

        Page<PostDetailResponse> posts = postQueryService.getPosts(
            keyword,
            category,
            boardCategory,
            publishOrigin,
            pageable,
            accountId
        );

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
            postRequest.tags(),
            postRequest.boardCategory(),
            postRequest.fileIds()
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

    @PutMapping("/{postId:\\d+}/purchase-vote")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PurchaseVoteResponse> votePurchase(
        @PathVariable("postId") Long postId,
        @RequestBody @Valid PurchaseVoteRequest request,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        PurchaseVoteSummary summary = purchaseVoteService.vote(postId, accountId(user), request.voteType());
        return ResponseEntity.ok(PurchaseVoteResponse.from(summary));
    }

    @DeleteMapping("/{postId:\\d+}/purchase-vote")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PurchaseVoteResponse> unvotePurchase(
        @PathVariable("postId") Long postId,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        PurchaseVoteSummary summary = purchaseVoteService.unvote(postId, accountId(user));
        return ResponseEntity.ok(PurchaseVoteResponse.from(summary));
    }

    private static Long optionalAccountId(UserPrincipal user) {
        return user != null ? accountId(user) : null;
    }

    private static Long accountId(UserPrincipal user) {
        return user.getAccountId();
    }
}
