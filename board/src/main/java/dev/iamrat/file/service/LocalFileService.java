package dev.iamrat.file.service;

import dev.iamrat.file.dto.FileDownloadResponse;
import dev.iamrat.file.dto.FileUploadResponse;
import dev.iamrat.file.entity.PostFile;
import dev.iamrat.file.repository.FileRepository;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Profile("local")
@RequiredArgsConstructor
public class LocalFileService {

    private final FileRepository fileRepository;

    @Value("${spring.file.upload-dir}")
    private String uploadDir;

    private static final Tika TIKA = new Tika();

    private static final Map<String, String> EXTENSION_MIME_MAP = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".gif", "image/gif",
            ".pdf", "application/pdf"
    );

    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_EMPTY);
        }

        String fileName = file.getOriginalFilename();
        String extension = extractExtension(fileName);
        String detectedMimeType = validateMimeType(file, extension);
        Path storagePath = buildStoragePath(extension);

        Files.createDirectories(storagePath.getParent());

        try {
            file.transferTo(storagePath.toFile());
        } catch (IOException e) {
            log.error("물리적 파일 저장 실패", e);
            throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
        }

        String savedName = storagePath.getFileName().toString();
        String relativePath = Paths.get(uploadDir).relativize(storagePath).toString();

        PostFile fileEntity = PostFile.builder()
                .originalFileName(fileName)
                .savedFileName(savedName)
                .filePath(relativePath)
                .fileSize(file.getSize())
                .fileType(detectedMimeType)
                .build();

        log.info("파일 DB 기록 성공: {}", fileEntity.getFilePath());

        fileRepository.save(fileEntity);

        return new FileUploadResponse(fileEntity.getId(), savedName, "/files/" + fileEntity.getId());
    }

    @Transactional(readOnly = true)
    public FileDownloadResponse downloadFile(Long fileId) throws IOException {
        PostFile fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        Path filePath = Paths.get(uploadDir)
            .resolve(fileEntity.getFilePath()).normalize();
        
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new CustomException(ErrorCode.FILE_READ_ERROR);
        }

        return new FileDownloadResponse(
                resource,
                fileEntity.getOriginalFileName(),
                fileEntity.getFileType()
        );
    }

    private String extractExtension(String fileName) {
        String extension = "";
        
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        }
        
        if (!EXTENSION_MIME_MAP.containsKey(extension)) {
            throw new CustomException(ErrorCode.FILE_EXTENSION_NOT_ALLOWED);
        }
        
        return extension;
    }
    
    private String validateMimeType(MultipartFile file, String extension) throws IOException {
        String detectedMimeType = TIKA.detect(file.getInputStream());
        String expectedMimeType = EXTENSION_MIME_MAP.get(extension);

        if (!detectedMimeType.equals(expectedMimeType)) {
            log.warn("파일 위조 의심! 확장자: {}, 실제 타입: {}", extension, detectedMimeType);
            throw new CustomException(ErrorCode.FILE_TYPE_MISMATCH);
        }

        return detectedMimeType;
    }

    private Path buildStoragePath(String extension) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String savedName = UUID.randomUUID() + extension;
        String relativePath = datePath + File.separator + savedName;

        return Paths.get(uploadDir).resolve(relativePath).normalize();
    }
}
