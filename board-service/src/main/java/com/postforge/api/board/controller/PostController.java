package com.postforge.api.board.controller;

import com.postforge.api.board.service.PostService;
import com.postforge.domain.board.dto.request.PostRequest;
import com.postforge.domain.board.dto.response.PostResponse;
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
@CrossOrigin(origins = "http://localhost:3000")
public class PostController {

    private final PostService postService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostResponse> createPost(
        @RequestBody @Valid PostRequest request,
        @AuthenticationPrincipal UserDetails user
    ) {
        PostResponse savedPost = postService.savePost(request.title(), request.content(), user.getUsername());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(savedPost);
    }

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPosts(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(postService.getPosts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @postService.isOwner(#id, authentication.name)")
    public ResponseEntity<String> updatePost(
        @PathVariable Long id,
        @RequestBody PostRequest request
    ) {
        postService.updatePost(id, request.title(), request.content());
        return ResponseEntity.ok("게시글 업데이트 완료");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @postService.isOwner(#id, authentication.name)")
    public ResponseEntity<String> deletePost(
        @PathVariable Long id
    ) {
        postService.deletePost(id);
        return ResponseEntity.ok("게시글 삭제 완료");
    }
}
