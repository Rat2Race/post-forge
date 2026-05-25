package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentStore {

    Comment save(Comment comment);

    void delete(Comment comment);

    Optional<Comment> findById(Long commentId);

    Comment getReferenceById(Long commentId);

    Page<Comment> findByPostId(Long postId, Pageable pageable);

    int countByPostId(Long postId);

    List<Object[]> countByPostIds(List<Long> postIds);

    void updateLikeCount(Long commentId, long likeCount);
}
