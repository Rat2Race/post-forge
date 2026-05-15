package dev.iamrat.board.file.controller;

import dev.iamrat.board.file.dto.FileUploadResponse;
import dev.iamrat.board.file.service.S3FileService;
import dev.iamrat.core.global.dto.UrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/files/s3")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
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
    public ResponseEntity<UrlResponse> getDownloadUrl(@PathVariable Long fileId) {
        String downloadUrl = fileService.createDownloadUrl(fileId);
        return ResponseEntity.ok(UrlResponse.of(downloadUrl));
    }
}
