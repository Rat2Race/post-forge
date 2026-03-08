package dev.iamrat.post.service;

import dev.iamrat.file.entity.PostFile;
import dev.iamrat.file.repository.FileRepository;
import dev.iamrat.post.dto.PostDetailResponse;
import dev.iamrat.post.dto.PostSummaryResponse;
import dev.iamrat.post.entity.Post;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.post.repository.PostLikeRepository;
import dev.iamrat.post.repository.PostRepository;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeService postLikeService;
    private final PostLikeRepository postLikeRepository;
    private final FileRepository fileRepository;

    @Transactional
    public PostSummaryResponse savePost(String title, String content, String userId, List<Long> fileIds) {
        Post newPost = Post.builder()
            .title(title)
            .content(content)
            .userId(userId)
            .build();

        postRepository.save(newPost);
        linkFiles(newPost, fileIds);

        return PostSummaryResponse.from(newPost);
    }

    public Page<PostDetailResponse> getPosts(Pageable pageable, String userId) {
        Page<Post> posts = postRepository.findAllWithDetails(pageable);
        Set<Long> likedPostIds = getLikedPostIds(posts, userId);

        return posts.map(post -> PostDetailResponse.from(post, likedPostIds.contains(post.getId()), post.getLikeCount()));
    }

    public Page<PostDetailResponse> searchPosts(String keyword, Pageable pageable, String userId) {
        Page<Post> posts = postRepository.findByKeyword(keyword, pageable);
        Set<Long> likedPostIds = getLikedPostIds(posts, userId);

        return posts.map(post -> PostDetailResponse.from(post, likedPostIds.contains(post.getId()), post.getLikeCount()));
    }

    @Transactional
    public PostDetailResponse getPost(Long postId, boolean incrementView, String userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (incrementView) {
            post.updateViews(post.getViews() + 1);
        }

        return getPostDetailResponse(post, userId);
    }

    @Transactional
    public PostSummaryResponse updatePost(Long postId, String title, String content, List<Long> fileIds) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        post.update(title, content);
        
        List<PostFile> existingFiles = fileRepository.findAllByPost(post);
        existingFiles.forEach(PostFile::unassignPost);

        linkFiles(post, fileIds);

        return PostSummaryResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        List<PostFile> files = fileRepository.findAllByPost(post);
        files.forEach(PostFile::unassignPost);

        postRepository.delete(post);
    }

    public boolean isOwner(Long postId, String userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        return post.getUserId().equals(userId);
    }

    private void linkFiles(Post post, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        List<PostFile> files = fileRepository.findAllByIdIn(fileIds);
        files.forEach(file -> file.assignPost(post));
    }

    private Set<Long> getLikedPostIds(Page<Post> posts, String userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        List<Long> postIds = posts.getContent().stream().map(Post::getId).toList();
        return Set.copyOf(postLikeRepository.findLikedPostIds(postIds, userId));
    }

    private PostDetailResponse getPostDetailResponse(Post post, String userId) {
        boolean isLiked = postLikeService.isLiked(post.getId(), userId);
        return PostDetailResponse.from(post, isLiked, post.getLikeCount());
    }
}
