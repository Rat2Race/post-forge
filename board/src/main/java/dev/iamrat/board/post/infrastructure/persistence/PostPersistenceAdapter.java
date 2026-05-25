package dev.iamrat.board.post.infrastructure.persistence;

import dev.iamrat.board.post.application.PostStore;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostPersistenceAdapter implements PostStore {

    private final PostRepository postRepository;

    @Override
    public Post save(Post post) {
        return postRepository.save(post);
    }

    @Override
    public void delete(Post post) {
        postRepository.delete(post);
    }

    @Override
    public Optional<Post> findById(Long postId) {
        return postRepository.findById(postId);
    }

    @Override
    public Iterable<Post> findAllById(List<Long> postIds) {
        return postRepository.findAllById(postIds);
    }

    @Override
    public boolean existsById(Long postId) {
        return postRepository.existsById(postId);
    }

    @Override
    public Post getReferenceById(Long postId) {
        return postRepository.getReferenceById(postId);
    }

    @Override
    public Page<Post> findAll(Pageable pageable) {
        return postRepository.findAll(pageable);
    }

    @Override
    public Page<Post> findByKeyword(String keyword, Pageable pageable) {
        return postRepository.findByKeyword(keyword, pageable);
    }

    @Override
    public void updateViews(Long postId, long views) {
        postRepository.updateViews(postId, views);
    }

    @Override
    public void updateLikeCount(Long postId, long likeCount) {
        postRepository.updateLikeCount(postId, likeCount);
    }
}
