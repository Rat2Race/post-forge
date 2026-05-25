package dev.iamrat.board.file.domain;

import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileTypePolicyTest {

    private final FileTypePolicy fileTypePolicy = new FileTypePolicy();

    @Test
    void validate_supportedFile_returnsExtensionMimeTypeAndFolder() {
        FileTypePolicy.ValidatedFileType fileType = fileTypePolicy.validate("photo.PNG", " IMAGE/PNG ");

        assertThat(fileType.extension()).isEqualTo(".png");
        assertThat(fileType.mimeType()).isEqualTo("image/png");
        assertThat(fileType.storageFolder()).isEqualTo("images");
    }

    @Test
    void validate_pdfFile_usesDocumentFolder() {
        FileTypePolicy.ValidatedFileType fileType = fileTypePolicy.validate("spec.pdf", "application/pdf");

        assertThat(fileType.storageFolder()).isEqualTo("documents");
    }

    @Test
    void validate_unsupportedExtension_throwsFileExtensionNotAllowed() {
        assertThatThrownBy(() -> fileTypePolicy.validate("script.exe", "application/octet-stream"))
            .isInstanceOf(CustomException.class)
            .extracting(error -> ((CustomException) error).getErrorCode())
            .isEqualTo(BoardErrorCode.FILE_EXTENSION_NOT_ALLOWED);
    }

    @Test
    void validate_mismatchedContentType_throwsFileTypeMismatch() {
        assertThatThrownBy(() -> fileTypePolicy.validate("photo.png", "image/jpeg"))
            .isInstanceOf(CustomException.class)
            .extracting(error -> ((CustomException) error).getErrorCode())
            .isEqualTo(BoardErrorCode.FILE_TYPE_MISMATCH);
    }
}
