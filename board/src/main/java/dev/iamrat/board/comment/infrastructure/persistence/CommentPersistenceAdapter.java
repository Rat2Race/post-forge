package dev.iamrat.board.comment.infrastructure.persistence;

import dev.iamrat.board.comment.application.CommentStore;
import dev.iamrat.board.comment.domain.Comment;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements CommentStore {

    private final CommentRepository commentRepository;

    @Override
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    @Override
    public void delete(Comment comment) {
        commentRepository.delete(comment);
    }

    @Override
    public Optional<Comment> findById(Long commentId) {
        return commentRepository.findById(commentId);
    }

    @Override
    public Comment getReferenceById(Long commentId) {
        return commentRepository.getReferenceById(commentId);
    }

    @Override
    public Page<Comment> findByPostId(Long postId, Pageable pageable) {
        return commentRepository.findByPostId(postId, pageable);
    }

    @Override
    public int countByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    @Override
    public List<Object[]> countByPostIds(List<Long> postIds) {
        return commentRepository.countByPostIds(postIds);
    }

    @Override
    public void updateLikeCount(Long commentId, long likeCount) {
        commentRepository.updateLikeCount(commentId, likeCount);
    }
}
