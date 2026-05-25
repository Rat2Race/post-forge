package dev.iamrat.board.file.application;

import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileReader {

    private final FileStore fileStore;
    private final FileStorage fileStorage;

    public String createDownloadUrl(Long fileId) {
        PostFile file = fileStore.findById(fileId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.FILE_NOT_FOUND));

        return fileStorage.createDownloadUrl(file.getFilePath());
    }
}
