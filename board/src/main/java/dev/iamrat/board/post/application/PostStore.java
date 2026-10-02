package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostType;
import dev.iamrat.core.board.post.NewsSection;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.time.LocalDateTime;
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

    List<Post> findByCategoryAndBoardCategoryInRange(
        PostType category,
        NewsSection boardCategory,
        LocalDateTime startInclusive,
        LocalDateTime endExclusive
    );

    boolean existsByCategoryAndBoardCategoryAndTitle(PostType category, NewsSection boardCategory, String title);

    Page<Post> findByFilters(
        String keyword,
        PostType category,
        NewsSection boardCategory,
        PostPublishOrigin publishOrigin,
        Pageable pageable
    );

    void updateViews(Long postId, long views);

    void updateLikeCount(Long postId, long likeCount);
}
