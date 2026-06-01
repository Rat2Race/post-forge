package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.comment.presentation.dto.CommentDetailResponse;
import dev.iamrat.board.like.application.CommentLikeService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final CommentStore commentStore;
    private final CommentLikeService commentLikeService;

    public Page<CommentDetailResponse> getCommentsByPost(Long postId, Pageable pageable, Long accountId) {
        Page<Comment> comments = commentStore.findByPostId(postId, pageable);
        List<Comment> commentList = comments.getContent();
        List<Long> commentIds = commentList.stream()
            .map(Comment::getId)
            .toList();

        Map<Long, Long> likeCounts = commentLikeService.getLikeCounts(commentIds);
        Set<Long> likedIds = commentLikeService.getLikedCommentIds(commentIds, accountId);

        return new PageImpl<>(
            commentList.stream()
                .map(comment -> CommentDetailResponse.from(
                    comment,
                    likeCounts.getOrDefault(comment.getId(), 0L),
                    likedIds.contains(comment.getId())
                ))
                .toList(),
            pageable,
            comments.getTotalElements()
        );
    }

    public int getCommentCount(Long postId) {
        return commentStore.countByPostId(postId);
    }

    public Map<Long, Integer> getCommentCounts(List<Long> postIds) {
        return commentStore.countByPostIds(postIds).stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> ((Long) row[1]).intValue()
            ));
    }
}
