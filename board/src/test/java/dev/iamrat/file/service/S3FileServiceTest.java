package dev.iamrat.file.service;

import dev.iamrat.file.dto.FileUploadResponse;
import dev.iamrat.file.entity.PostFile;
import dev.iamrat.file.repository.FileRepository;
import dev.iamrat.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3FileServiceTest {

    @InjectMocks
    private S3FileService s3FileService;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private FileRepository fileRepository;

    @Test
    @DisplayName("Presigned Upload URL 생성 시 파일 저장 후 URL 반환")
    void testCreatePresignedUrl() throws MalformedURLException {
        PostFile savedFile = PostFile.builder()
                .originalFileName("photo.png")
                .savedFileName("uuid.png")
                .filePath("images/2026-03-08/uuid.png")
                .fileType("image/png")
                .build();

        given(fileRepository.save(any(PostFile.class))).willReturn(savedFile);

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        given(presignedRequest.url()).willReturn(URI.create("https://s3.example.com/presigned").toURL());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedRequest);

        FileUploadResponse response = s3FileService.createPresignedUrl("photo.png", "image/png");

        assertThat(response.savedName()).endsWith(".png");
        assertThat(response.url()).isEqualTo("https://s3.example.com/presigned");
        verify(fileRepository).save(any(PostFile.class));
    }

    @Test
    @DisplayName("Presigned Download URL 생성 시 파일 조회 후 URL 반환")
    void testCreateDownloadUrl() throws MalformedURLException {
        PostFile file = PostFile.builder()
                .originalFileName("photo.png")
                .savedFileName("uuid.png")
                .filePath("images/2026-03-08/uuid.png")
                .fileType("image/png")
                .build();

        given(fileRepository.findById(1L)).willReturn(Optional.of(file));

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        given(presignedRequest.url()).willReturn(URI.create("https://s3.example.com/download").toURL());
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presignedRequest);

        String downloadUrl = s3FileService.createDownloadUrl(1L);

        assertThat(downloadUrl).isEqualTo("https://s3.example.com/download");
        verify(fileRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 파일 다운로드 시 예외 발생")
    void testCreateDownloadUrl_fileNotFound() {
        given(fileRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> s3FileService.createDownloadUrl(999L))
                .isInstanceOf(CustomException.class);
    }
}
