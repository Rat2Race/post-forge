package dev.iamrat.file.controller;

import dev.iamrat.file.dto.FileDownloadResponse;
import dev.iamrat.file.dto.FileUploadResponse;
import dev.iamrat.file.service.LocalFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/files/local")
@Profile("local")
@RequiredArgsConstructor
public class LocalFileController {

    private final LocalFileService localFileService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(localFileService.uploadFile(file));
    }

    @GetMapping("download/{fileId}")
    public ResponseEntity<Resource> getFile(
            @PathVariable Long fileId,
            @RequestParam(value = "download", defaultValue = "false") boolean download) throws IOException {

        FileDownloadResponse file = localFileService.downloadFile(fileId);
        Resource resource = file.resource();
        String encodedFileName = URLEncoder.encode(file.originalName(), StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();

        if (download) {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFileName);
        } else {
            headers.setContentType(MediaType.parseMediaType(file.mimeType()));
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}
