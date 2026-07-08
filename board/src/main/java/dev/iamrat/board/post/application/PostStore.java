package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.PostBoardCategory;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostStore {

    Post save(Post post);

    void delete(Post post);

    Optional<Post> findById(Long postId);

    Iterable<Post> findAllById(List<Long> postIds);

    boolean existsById(Long postId);

    Post getReferenceById(Long postId);

    Page<Post> findAll(Pageable pageable);

    Page<Post> findByFilters(
        String keyword,
        PostCategory category,
        PostBoardCategory boardCategory,
        PostPublishOrigin publishOrigin,
        Pageable pageable
    );

    void updateViews(Long postId, long views);

    void incrementViews(Long postId);

    void updateLikeCount(Long postId, long likeCount);
}
