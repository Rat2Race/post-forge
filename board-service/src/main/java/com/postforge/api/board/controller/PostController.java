package com.postforge.api.board.controller;

import com.postforge.api.board.service.PostService;
import com.postforge.api.board.service.ViewCountService;
import com.postforge.domain.board.dto.request.PostRequest;
import com.postforge.domain.board.dto.response.PostResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
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
@CrossOrigin(origins = "http://localhost:3000")
public class PostController {

    private final PostService postService;
    private final ViewCountService viewCountService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostResponse> createPost(
        @RequestBody @Valid PostRequest postRequest,
        @AuthenticationPrincipal UserDetails user
    ) {
        PostResponse savedPost = postService.savePost(
            postRequest.title(),
            postRequest.content(),
            user.getUsername()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(savedPost);
    }

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPosts(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostResponse> posts = postService.getPosts(pageable);

        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(
        @PathVariable Long postId,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        Cookie[] cookies = servletRequest.getCookies();

        boolean shouldIncrement = viewCountService.shouldIncrementView(postId, cookies);
        PostResponse post = postService.getPost(postId, shouldIncrement);

        if(shouldIncrement) {
            Cookie cookie = viewCountService.createViewCookie(postId);
            servletResponse.addCookie(cookie);
        }

        return ResponseEntity.ok(post);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @postService.isOwner(#id, authentication.name)")
    public ResponseEntity<String> updatePost(
        @PathVariable Long postId,
        @RequestBody PostRequest postRequest
    ) {
        postService.updatePost(
            postId,
            postRequest.title(),
            postRequest.content()
        );

        return ResponseEntity.ok("게시글 업데이트 완료");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @postService.isOwner(#id, authentication.name)")
    public ResponseEntity<String> deletePost(
        @PathVariable Long postId
    ) {
        postService.deletePost(postId);

        return ResponseEntity.ok("게시글 삭제 완료");
    }
}
