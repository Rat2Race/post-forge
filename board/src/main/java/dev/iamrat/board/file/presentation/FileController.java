package dev.iamrat.board.file.presentation;

import dev.iamrat.board.file.application.FileReader;
import dev.iamrat.board.file.application.FileUploadResponse;
import dev.iamrat.board.file.application.FileUploadService;
import dev.iamrat.core.global.dto.UrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/files", "/files/s3"})
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;
    private final FileReader fileReader;

    @GetMapping("/presigned-url")
    public ResponseEntity<FileUploadResponse> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {
        FileUploadResponse response = fileUploadService.createPresignedUrl(fileName, contentType);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{fileId}/download-url")
    public ResponseEntity<UrlResponse> getDownloadUrl(@PathVariable Long fileId) {
        String downloadUrl = fileReader.createDownloadUrl(fileId);
        return ResponseEntity.ok(UrlResponse.of(downloadUrl));
    }
}
