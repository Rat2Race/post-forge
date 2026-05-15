package dev.iamrat.board.port.post;

import dev.iamrat.core.board.post.PostWriteCommand;
import dev.iamrat.core.board.post.PostWriter;
import dev.iamrat.board.post.entity.Post;
import dev.iamrat.board.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BoardPostWriter implements PostWriter {

    private final PostRepository postRepository;

    @Override
    @Transactional
    public Long write(PostWriteCommand command) {
        Post post = Post.builder()
                .title(command.title())
                .content(command.content())
                .summary(command.summary())
                .tags(command.tags())
                .category(command.category())
                .userId(command.userId())
                .nickname(command.nickname())
                .build();
        return postRepository.save(post).getId();
    }
}
