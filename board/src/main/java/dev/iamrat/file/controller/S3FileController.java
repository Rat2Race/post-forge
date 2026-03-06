package dev.iamrat.file.controller;

import dev.iamrat.file.dto.FileUploadResponse;
import dev.iamrat.file.service.S3FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/files/s3")
@Profile("s3")
@RequiredArgsConstructor
public class S3FileController {

    private final S3FileService fileService;

    @GetMapping("/presigned-url")
    public ResponseEntity<FileUploadResponse> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {
        FileUploadResponse response = fileService.createPresignedUrl(fileName, contentType);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{fileId}/download-url")
    public ResponseEntity<String> getDownloadUrl(@PathVariable Long fileId) {
        String downloadUrl = fileService.createDownloadUrl(fileId);
        return ResponseEntity.ok(downloadUrl);
    }
}
