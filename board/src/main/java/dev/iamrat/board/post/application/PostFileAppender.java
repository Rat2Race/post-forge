package dev.iamrat.board.post.application;

import dev.iamrat.board.file.application.FileAttachmentService;
import dev.iamrat.board.post.domain.Post;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostFileAppender {

    private final FileAttachmentService fileAttachmentService;

    public void appendFiles(Post post, List<Long> fileIds) {
        fileAttachmentService.appendFiles(post, fileIds);
    }

    public void replaceFiles(Post post, List<Long> fileIds) {
        fileAttachmentService.replaceFiles(post, fileIds);
    }

    public void detachFiles(Post post) {
        fileAttachmentService.detachFiles(post);
    }
}
