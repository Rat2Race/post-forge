package dev.iamrat.board.post.infrastructure.persistence;

import dev.iamrat.board.post.application.PostStore;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
        return postRepository.findAll(keywordFilter(keyword), pageable);
    }

    @Override
    public void updateViews(Long postId, long views) {
        postRepository.updateViews(postId, views);
    }

    @Override
    public void addLikeCount(Long postId, long delta) {
        postRepository.addLikeCount(postId, delta);
    }

    private Specification<Post> keywordFilter(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), pattern)
            );
        };
    }
}
