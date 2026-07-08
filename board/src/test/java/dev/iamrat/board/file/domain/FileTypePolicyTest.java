package dev.iamrat.board.file.domain;

import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileTypePolicyTest {

    private final FileTypePolicy fileTypePolicy = new FileTypePolicy();

    @Test
    @DisplayName("지원 파일이면 확장자, MIME type, 저장 폴더를 반환한다")
    void validate_supportedFile_returnsExtensionMimeTypeAndFolder() {
        FileTypePolicy.ValidatedFileType fileType = fileTypePolicy.validate("photo.PNG", " IMAGE/PNG ");

        assertThat(fileType.extension()).isEqualTo(".png");
        assertThat(fileType.mimeType()).isEqualTo("image/png");
        assertThat(fileType.storageFolder()).isEqualTo("images");
    }

    @Test
    @DisplayName("PDF 파일은 문서 폴더를 사용한다")
    void validate_pdfFile_usesDocumentFolder() {
        FileTypePolicy.ValidatedFileType fileType = fileTypePolicy.validate("spec.pdf", "application/pdf");

        assertThat(fileType.storageFolder()).isEqualTo("documents");
    }

    @Test
    @DisplayName("지원하지 않는 확장자는 허용되지 않은 확장자 예외를 던진다")
    void validate_unsupportedExtension_throwsFileExtensionNotAllowed() {
        assertThatThrownBy(() -> fileTypePolicy.validate("script.exe", "application/octet-stream"))
            .isInstanceOf(CustomException.class)
            .extracting(error -> ((CustomException) error).getErrorCode())
            .isEqualTo(BoardErrorCode.FILE_EXTENSION_NOT_ALLOWED);
    }

    @Test
    @DisplayName("확장자와 content type이 맞지 않으면 파일 타입 불일치 예외를 던진다")
    void validate_mismatchedContentType_throwsFileTypeMismatch() {
        assertThatThrownBy(() -> fileTypePolicy.validate("photo.png", "image/jpeg"))
            .isInstanceOf(CustomException.class)
            .extracting(error -> ((CustomException) error).getErrorCode())
            .isEqualTo(BoardErrorCode.FILE_TYPE_MISMATCH);
    }
}
