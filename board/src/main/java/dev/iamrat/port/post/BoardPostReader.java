package dev.iamrat.port.post;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.post.PostContext;
import dev.iamrat.post.PostReader;
import dev.iamrat.post.entity.Post;
import dev.iamrat.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BoardPostReader implements PostReader {

    private final PostRepository postRepository;
    
    @Override
    @Transactional(readOnly = true)
    public PostContext read(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        return new PostContext(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getSummary(),
            post.getTags(),
            post.getUserId(),
            post.getNickname()
        );
    }
}
