package dev.iamrat.board.file.infrastructure.persistence;

import dev.iamrat.board.file.application.FileStore;
import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.post.domain.Post;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FilePersistenceAdapter implements FileStore {

    private final FileRepository fileRepository;

    @Override
    public PostFile save(PostFile file) {
        return fileRepository.save(file);
    }

    @Override
    public Optional<PostFile> findById(Long fileId) {
        return fileRepository.findById(fileId);
    }

    @Override
    public List<PostFile> findAllByIdIn(List<Long> fileIds) {
        return fileRepository.findAllByIdIn(fileIds);
    }

    @Override
    public List<PostFile> findAllByPost(Post post) {
        return fileRepository.findAllByPost(post);
    }

    @Override
    public int deleteOrphanFilesBefore(LocalDateTime threshold) {
        return fileRepository.deleteOrphanFiles(threshold);
    }
}
