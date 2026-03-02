package dev.iamrat.file.service;

import dev.iamrat.file.dto.FileDto;
import dev.iamrat.file.dto.StoredFileInfo;
import dev.iamrat.file.entity.FileEntity;
import dev.iamrat.file.repository.FileRepository;
import dev.iamrat.file.util.LocalFileStorageManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final LocalFileStorageManager storageManager;
    private static final Map<String, String> EXTENSION_MIME_MAP = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".gif", "image/gif",
            ".pdf", "application/pdf"
    );

    @Transactional
    public Long uploadFile(MultipartFile file) throws IOException {
        if(file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있음");
        }

        String fileName = file.getOriginalFilename();
        String extension = extractExtension(fileName).toLowerCase();

        if (!EXTENSION_MIME_MAP.containsKey(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다: " + extension);
        }

        Tika tika = new Tika();
        String detectedMimeType = tika.detect(file.getInputStream());

        String expectedMimeType = EXTENSION_MIME_MAP.get(extension);

        if(!detectedMimeType.equals(expectedMimeType)) {
            log.warn("파일 위조 의심! 확장자: {}, 실제 타입: {}", extension, detectedMimeType);
            throw new IllegalArgumentException("파일 확장자와 실제 내용이 일치하지 않습니다.");
        }

        StoredFileInfo fileInfo = storageManager.storeFile(file, extension);

        FileEntity fileEntity = FileEntity.builder()
                .originalFileName(fileName)
                .savedFileName(fileInfo.savedName())
                .filePath(fileInfo.relativePath())
                .fileSize(file.getSize())
                .fileType(detectedMimeType)
                .build();

        log.info("파일 DB 기록 성공: {}", fileEntity.getFilePath());

        fileRepository.save(fileEntity);

        return fileEntity.getId();
    }

    @Transactional(readOnly = true)
    public FileDto getFile(Long fileId) throws IOException {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파일입니다. ID: " + fileId));

        Resource resource = storageManager.loadAsResource(fileEntity.getFilePath());

        if(!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("물리적 파일을 읽을 수 없습니다.");
        }

        return new FileDto(
                resource,
                fileEntity.getOriginalFileName(),
                fileEntity.getFileType()
        );
    }

    private String extractExtension(String fileName) {
        if(fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

}
