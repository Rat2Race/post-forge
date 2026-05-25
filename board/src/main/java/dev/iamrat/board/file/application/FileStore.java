package dev.iamrat.board.file.application;

import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.post.domain.Post;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileStore {

    PostFile save(PostFile file);

    Optional<PostFile> findById(Long fileId);

    List<PostFile> findAllByIdIn(List<Long> fileIds);

    List<PostFile> findAllByPost(Post post);

    int deleteOrphanFilesBefore(LocalDateTime threshold);
}
