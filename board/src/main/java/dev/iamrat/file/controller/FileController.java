package dev.iamrat.file.controller;

import dev.iamrat.file.dto.FileDto;
import dev.iamrat.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<Long> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        System.out.println("넘어온 파일 이름: " + file.getOriginalFilename());
        System.out.println("넘어온 파일 크기: " + file.getSize() + " 바이트");
        return ResponseEntity.ok(fileService.uploadFile(file));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> getFile(
            @PathVariable Long fileId,
            @RequestParam(value = "download", defaultValue = "false") boolean download) throws IOException {

        FileDto file = fileService.getFile(fileId);
        Resource resource = file.resource();

        String encodedFileName = URLEncoder.encode(file.originalName());

        HttpHeaders headers = new HttpHeaders();

        if(download) {
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
