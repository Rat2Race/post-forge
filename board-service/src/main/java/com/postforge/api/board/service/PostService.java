package com.postforge.api.board.service;

import com.postforge.domain.board.dto.request.PostRequest;
import com.postforge.domain.board.dto.response.PostResponse;
import com.postforge.domain.board.entity.Post;
import com.postforge.domain.board.repository.PostRepository;
import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    @Transactional
    public PostResponse savePost(String title, String content, String userId) {
        Post newPost = Post.builder()
            .title(title)
            .content(content)
            .userId(userId)
            .build();

        postRepository.save(newPost);

        return PostResponse.from(newPost);
    }

    public Page<PostResponse> getPosts(Pageable pageable) {
        return postRepository.findAll(pageable)
            .map(PostResponse::from);
    }

    public PostResponse getPost(Long postId) {
        return postRepository.findById(postId)
            .map(PostResponse::from)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    @Transactional
    public PostResponse updatePost(Long postId, String title, String content) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        post.update(title, content);

        return PostResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        postRepository.delete(post);
    }

    public boolean isOwner(Long postId, String userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        return post.getUserId().equals(userId);
    }
}
