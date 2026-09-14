package dev.iamrat.board.file.application;

import dev.iamrat.board.file.domain.FileTypePolicy;
import dev.iamrat.board.file.domain.FileTypePolicy.ValidatedFileType;
import dev.iamrat.board.file.domain.PostFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileStore fileStore;
    private final FileStorage fileStorage;
    private final FileTypePolicy fileTypePolicy = new FileTypePolicy();

    public FileUploadResult createPresignedUrl(String fileName, String contentType) {
        ValidatedFileType fileType = fileTypePolicy.validate(fileName, contentType);

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String savedFileName = UUID.randomUUID() + fileType.extension();
        String objectKey = fileType.storageFolder() + "/" + datePath + "/" + savedFileName;

        PostFile fileEntity = PostFile.builder()
            .originalFileName(fileName)
            .savedFileName(savedFileName)
            .filePath(objectKey)
            .fileType(fileType.mimeType())
            .build();
        PostFile saved = fileStore.save(fileEntity);

        String uploadUrl = fileStorage.createUploadUrl(objectKey, fileType.mimeType());
        return new FileUploadResult(saved.getId(), savedFileName, uploadUrl);
    }
}
