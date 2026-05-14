package dev.iamrat.port.post;

import dev.iamrat.board.post.PostWriteCommand;
import dev.iamrat.board.post.PostWriter;
import dev.iamrat.post.entity.Post;
import dev.iamrat.post.repository.PostRepository;
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
