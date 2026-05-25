package dev.iamrat.board.file.application;

import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    private FileUploadService fileUploadService;

    @Mock
    private FileStore fileStore;

    @Mock
    private FileStorage fileStorage;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService(fileStore, fileStorage);
    }

    @Test
    @DisplayName("Presigned Upload URL 생성 시 파일 저장 후 URL 반환")
    void testCreatePresignedUrl() {
        PostFile savedFile = PostFile.builder()
                .id(1L)
                .originalFileName("photo.png")
                .savedFileName("uuid.png")
                .filePath("images/2026-03-08/uuid.png")
                .fileType("image/png")
                .build();

        given(fileStore.save(any(PostFile.class))).willReturn(savedFile);
        given(fileStorage.createUploadUrl(any(), eq("image/png"))).willReturn("https://storage.example.com/presigned");

        FileUploadResponse response = fileUploadService.createPresignedUrl("photo.png", "image/png");

        ArgumentCaptor<PostFile> fileCaptor = ArgumentCaptor.forClass(PostFile.class);
        verify(fileStore).save(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getFilePath()).startsWith("images/");
        assertThat(fileCaptor.getValue().getFileType()).isEqualTo("image/png");
        assertThat(response.fileId()).isEqualTo(1L);
        assertThat(response.savedName()).endsWith(".png");
        assertThat(response.url()).isEqualTo("https://storage.example.com/presigned");
    }

    @Test
    @DisplayName("허용되지 않은 확장자는 Presigned URL을 발급하지 않는다")
    void testCreatePresignedUrl_rejectsUnsupportedExtension() {
        assertThatThrownBy(() -> fileUploadService.createPresignedUrl("script.exe", "application/octet-stream"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(BoardErrorCode.FILE_EXTENSION_NOT_ALLOWED);
    }

    @Test
    @DisplayName("확장자와 contentType이 다르면 Presigned URL을 발급하지 않는다")
    void testCreatePresignedUrl_rejectsMismatchedContentType() {
        assertThatThrownBy(() -> fileUploadService.createPresignedUrl("photo.png", "image/jpeg"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(BoardErrorCode.FILE_TYPE_MISMATCH);
    }
}
