package com.postforge.api.board.controller;

import com.postforge.api.board.service.PostLikeService;
import com.postforge.api.board.service.PostService;
import com.postforge.api.board.service.ViewCountService;
import com.postforge.domain.board.dto.request.PostRequest;
import com.postforge.domain.board.dto.response.LikeResponse;
import com.postforge.domain.board.dto.response.PostDetailResponse;
import com.postforge.domain.board.dto.response.PostSummaryResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final PostLikeService postLikeService;
    private final ViewCountService viewCountService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostSummaryResponse> createPost(
        @RequestBody @Valid PostRequest postRequest,
        @AuthenticationPrincipal UserDetails user
    ) {
        PostSummaryResponse savedPost = postService.savePost(
            postRequest.title(),
            postRequest.content(),
            user.getUsername()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(savedPost);
    }

    @GetMapping
    public ResponseEntity<Page<PostDetailResponse>> getPosts(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @AuthenticationPrincipal UserDetails user
    ) {
        String userId = user != null ? user.getUsername() : null;
        Page<PostDetailResponse> posts = postService.getPosts(pageable, userId);

        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{postId:\\d+}")
    public ResponseEntity<PostDetailResponse> getPost(
        @PathVariable("postId") Long postId,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse,
        @AuthenticationPrincipal UserDetails user
    ) {
        Cookie[] cookies = servletRequest.getCookies();

        boolean shouldIncrement = viewCountService.shouldIncrementView(postId, cookies);
        String userId = user != null ? user.getUsername() : null;

        PostDetailResponse post = postService.getPost(postId, shouldIncrement, userId);

        if(shouldIncrement) {
            Cookie cookie = viewCountService.createViewCookie(postId);
            servletResponse.addCookie(cookie);
        }

        return ResponseEntity.ok(post);
    }

    @PutMapping("/{postId:\\d+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @postService.isOwner(#postId, authentication.name)")
    public ResponseEntity<PostSummaryResponse> updatePost(
        @PathVariable("postId") Long postId,
        @RequestBody PostRequest postRequest,
        @AuthenticationPrincipal UserDetails user
    ) {
        PostSummaryResponse response = postService.updatePost(
            postId,
            postRequest.title(),
            postRequest.content(),
            user.getUsername()
        );

        return ResponseEntity.ok(response);
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
    public ResponseEntity<LikeResponse> toggleLike(
        @PathVariable("postId") Long postId,
        @AuthenticationPrincipal UserDetails user
    ) {
        LikeResponse response = postLikeService.toggleLike(postId, user.getUsername());

        return ResponseEntity.ok(response);
    }
}
