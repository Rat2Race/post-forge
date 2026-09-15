package dev.iamrat.board.file.application;

import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileAttachmentService {

    private final FileStore fileStore;

    public void appendFiles(Post post, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        fileStore.findAllByIdIn(fileIds)
            .forEach(file -> file.assignPost(post));
    }

    public void replaceFiles(Post post, List<Long> fileIds) {
        detachFiles(post);
        appendFiles(post, fileIds);
    }

    public void detachFiles(Post post) {
        fileStore.findAllByPost(post)
            .forEach(PostFile::unassignPost);
    }
}
