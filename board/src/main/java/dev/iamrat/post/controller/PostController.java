package dev.iamrat.post.controller;

import dev.iamrat.like.dto.LikeResponse;
import dev.iamrat.global.dto.PageResponse;
import dev.iamrat.post.service.PostService;
import dev.iamrat.post.dto.PostRequest;
import dev.iamrat.post.dto.PostDetailResponse;
import dev.iamrat.post.dto.PostSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import dev.iamrat.auth.security.dto.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostSummaryResponse> createPost(
        @RequestBody @Valid PostRequest postRequest,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        PostSummaryResponse savedPost = postService.savePost(
            postRequest.title(),
            postRequest.content(),
            user.getUserId(),
            user.getNickname(),
            postRequest.fileIds()
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
        String userId = user != null
            ? user.getUserId()
            : null;
        
        Page<PostDetailResponse> posts = keyword != null
            ? postService.searchPosts(keyword, pageable, userId)
            : postService.getPosts(pageable, userId);

        return ResponseEntity.ok(PageResponse.from(posts));
    }

    @GetMapping("/{postId:\\d+}")
    public ResponseEntity<PostDetailResponse> getPost(
        @PathVariable("postId") Long postId,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        String userId = user != null ? user.getUserId() : null;
        PostDetailResponse post = postService.readPost(postId, userId);

        return ResponseEntity.ok(post);
    }

    @PutMapping("/{postId:\\d+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @postService.isOwner(#postId, authentication.name)")
    public ResponseEntity<PostSummaryResponse> updatePost(
        @PathVariable("postId") Long postId,
        @RequestBody PostRequest postRequest
    ) {
        PostSummaryResponse modifiedPost = postService.updatePost(
            postId,
            postRequest.title(),
            postRequest.content(),
            postRequest.fileIds()
        );

        return ResponseEntity.ok(modifiedPost);
    }

    @DeleteMapping("/{postId:\\d+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @postService.isOwner(#postId, authentication.name)")
    public ResponseEntity<String> deletePost(
        @PathVariable("postId") Long postId
    ) {
        postService.deletePost(postId);

        return ResponseEntity.ok("게시글 삭제 완료");
    }

    @PostMapping("/{postId:\\d+}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> likePost(
        @PathVariable("postId") Long postId,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        LikeResponse likeStatus = postService.likePost(postId, user.getUserId());

        return ResponseEntity.ok(likeStatus);
    }

    @DeleteMapping("/{postId:\\d+}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> unlikePost(
        @PathVariable("postId") Long postId,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        LikeResponse likeStatus = postService.unlikePost(postId, user.getUserId());

        return ResponseEntity.ok(likeStatus);
    }
}
