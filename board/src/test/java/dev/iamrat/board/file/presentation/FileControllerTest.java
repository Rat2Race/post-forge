package dev.iamrat.board.file.presentation;

import dev.iamrat.board.file.application.FileReader;
import dev.iamrat.board.file.application.FileUploadResponse;
import dev.iamrat.board.file.application.FileUploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileUploadService fileUploadService;

    @MockitoBean
    private FileReader fileReader;

    @Test
    @DisplayName("storage-neutral 경로에서 Presigned URL 발급 요청 시 FileUploadResponse 반환")
    void getPresignedUrl_storageNeutralPath_returnsFileUploadResponse() throws Exception {
        FileUploadResponse response = new FileUploadResponse(1L, "uuid.jpg", "https://s3.presigned-url.example.com");
        given(fileUploadService.createPresignedUrl("test.jpg", "image/jpeg")).willReturn(response);

        mockMvc.perform(get("/files/presigned-url")
                        .param("fileName", "test.jpg")
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(1))
                .andExpect(jsonPath("$.savedName").value("uuid.jpg"))
                .andExpect(jsonPath("$.url").value("https://s3.presigned-url.example.com"));
    }

    @Test
    @DisplayName("legacy S3 경로도 Presigned URL 발급을 유지한다")
    void getPresignedUrl_legacyS3Path_returnsFileUploadResponse() throws Exception {
        FileUploadResponse response = new FileUploadResponse(1L, "uuid.jpg", "https://s3.presigned-url.example.com");
        given(fileUploadService.createPresignedUrl("test.jpg", "image/jpeg")).willReturn(response);

        mockMvc.perform(get("/files/s3/presigned-url")
                        .param("fileName", "test.jpg")
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(1))
                .andExpect(jsonPath("$.savedName").value("uuid.jpg"))
                .andExpect(jsonPath("$.url").value("https://s3.presigned-url.example.com"));
    }

    @Test
    @DisplayName("storage-neutral 경로에서 다운로드 URL 요청 시 Presigned Download URL 반환")
    void getDownloadUrl_storageNeutralPath_returnsDownloadUrl() throws Exception {
        given(fileReader.createDownloadUrl(1L)).willReturn("https://s3.download-url.example.com");

        mockMvc.perform(get("/files/1/download-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://s3.download-url.example.com"));
    }
}
