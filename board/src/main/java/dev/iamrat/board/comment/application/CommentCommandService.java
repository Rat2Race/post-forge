package dev.iamrat.board.comment.application;

import dev.iamrat.board.comment.domain.Comment;
import dev.iamrat.board.comment.domain.CommentPolicy;
import dev.iamrat.board.comment.dto.CommentSummaryResponse;
import dev.iamrat.board.post.application.PostReader;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.account.AccountProfileReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentCommandService {

    private final CommentStore commentStore;
    private final CommentReader commentReader;
    private final PostReader postReader;
    private final AccountProfileReader accountProfileReader;
    private final CommentPolicy commentPolicy = new CommentPolicy();

    @Transactional
    public CommentSummaryResponse saveComment(Long postId, Long parentId, String content, Long accountId) {
        commentPolicy.validateAuthor(accountId);
        String nickname = accountProfileReader.getProfile(accountId).nickname();

        Post post = postReader.getById(postId);
        Comment parent = resolveParent(postId, parentId);

        Comment newComment = Comment.create(post, parent, content, accountId, nickname);

        if (parent != null) {
            parent.addReply(newComment);
        }

        post.addComment(newComment);
        commentStore.save(newComment);

        return CommentSummaryResponse.from(newComment);
    }

    @Transactional
    public CommentSummaryResponse updateComment(Long commentId, String newContent) {
        Comment comment = commentReader.getById(commentId);

        comment.updateContent(newContent);

        return CommentSummaryResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        commentStore.delete(commentReader.getById(commentId));
    }

    public boolean isCommentOwner(Long commentId, Long accountId) {
        return commentPolicy.isOwner(commentReader.getById(commentId), accountId);
    }

    private Comment resolveParent(Long postId, Long parentId) {
        if (parentId == null) {
            return null;
        }

        Comment parent = commentReader.getById(parentId);
        commentPolicy.validateParent(postId, parent);
        return parent;
    }
}
