package dev.iamrat.file.util;

import dev.iamrat.file.dto.StoredFileInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
public class LocalFileStorageManager {

    @Value("${spring.file.upload-dir}")
    private String uploadDir;

    public StoredFileInfo storeFile(MultipartFile file, String extension) throws IOException {
        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String savedName = UUID.randomUUID() + extension;
            String relativePath = datePath + File.separator + savedName;

            Path fullPath = Paths.get(uploadDir).resolve(relativePath).normalize();
            Files.createDirectories(fullPath.getParent());

            file.transferTo(fullPath.toFile());

            return new StoredFileInfo(savedName, relativePath, fullPath.toString());
        } catch (IOException e) {
            log.error("물리적 파일 저장 실패", e);
            throw new RuntimeException("파일 저장 중 시스템 오류가 발생했습니다.");
        }
    }

    public Resource loadAsResource(String savedPath) throws IOException {
        Path filePath = Paths.get(savedPath);

        if(!filePath.isAbsolute()) {
            filePath = Paths.get(uploadDir).resolve(savedPath).normalize();
        }

        Resource resource = new UrlResource(filePath.toUri());

        if(!resource.exists() || !resource.isReadable()) {
            throw new IOException("파일을 읽을 수 없습니다: " + filePath);
        }

        return resource;
    }
}
