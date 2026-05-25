package dev.iamrat.board.file.application;

import dev.iamrat.board.file.domain.PostFile;
import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileReaderTest {

    @InjectMocks
    private FileReader fileReader;

    @Mock
    private FileStore fileStore;

    @Mock
    private FileStorage fileStorage;

    @Test
    @DisplayName("Presigned Download URL 생성 시 파일 조회 후 URL 반환")
    void createDownloadUrl_existingFile_returnsDownloadUrl() {
        PostFile file = PostFile.builder()
            .originalFileName("photo.png")
            .savedFileName("uuid.png")
            .filePath("images/2026-03-08/uuid.png")
            .fileType("image/png")
            .build();

        given(fileStore.findById(1L)).willReturn(Optional.of(file));
        given(fileStorage.createDownloadUrl("images/2026-03-08/uuid.png"))
            .willReturn("https://storage.example.com/download");

        String downloadUrl = fileReader.createDownloadUrl(1L);

        assertThat(downloadUrl).isEqualTo("https://storage.example.com/download");
        verify(fileStore).findById(1L);
        verify(fileStorage).createDownloadUrl("images/2026-03-08/uuid.png");
    }

    @Test
    @DisplayName("존재하지 않는 파일 다운로드 시 FILE_NOT_FOUND 예외 발생")
    void createDownloadUrl_missingFile_throwsFileNotFound() {
        given(fileStore.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> fileReader.createDownloadUrl(999L))
            .isInstanceOf(CustomException.class)
            .extracting(error -> ((CustomException) error).getErrorCode())
            .isEqualTo(BoardErrorCode.FILE_NOT_FOUND);
    }
}
